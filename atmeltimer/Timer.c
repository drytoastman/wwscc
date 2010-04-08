
#include "Timer.h"

// Scheduler Task List
TASK_LIST
{
	{ Task: USB_USBTask  , TaskStatus: TASK_STOP },
	{ Task: Timer_Task   , TaskStatus: TASK_STOP },
};

// Globals: 
CDC_Line_Coding_t LineCoding = { BaudRateBPS: 9600,
                                 CharFormat:  OneStopBit,
                                 ParityType:  Parity_None,
                                 DataBits:    8            };


#define TIMER_COUNT 4
static unsigned long timer = 0;
static struct
{
	bool sendTime;
	unsigned char mask;
	unsigned char timestring[8];
	unsigned long lasttime;
} Timers[TIMER_COUNT];


static struct
{
	unsigned char flag;
	unsigned long time;
	unsigned char string[8];
} Update;



static inline void timerProcess(int x)
{
	*(unsigned long *)&(Timers[x].timestring[2]) = timer; 
	Timers[x].lasttime = timer;
	Timers[x].sendTime = true;
	EIMSK &= ~Timers[x].mask;  
}


ISR(TIMER0_COMPA_vect)
{
	timer++;
	for (int ii = 0; ii < TIMER_COUNT; ii++)
	{
		if ((!(EIMSK & Timers[ii].mask)) && (Timers[ii].lasttime + 30000 < timer))
		{
			EIMSK |= Timers[ii].mask; // renable interrupt
			EIFR = Timers[ii].mask; // also clear flag that may have been set during off time
		}
	}

	if (Update.time + 321 < timer)  // cause seemingly random update on display
	{
		Update.time = timer; 
		*(unsigned long *)&(Update.string[2]) = timer; 
		Update.flag = true;
	}
}

ISR(TIMER1_OVF_vect)
{
	if (PORTD & _BV(PIN4))
		PORTD &= ~_BV(PIN4);
	else
		PORTD |= _BV(PIN4);
}

ISR(INT0_vect) { timerProcess(0); } // PD0
ISR(INT1_vect) { timerProcess(1); } // PD1
ISR(INT2_vect) { timerProcess(2); } // PD2
ISR(INT3_vect) { timerProcess(3); } // PD3
#if TIMER_COUNT > 4
ISR(INT4_vect) { timerProcess(4); } // PC7
#if TIMER_COUNT > 5
ISR(INT6_vect) { timerProcess(5); } // PD6
#endif
#endif


int main(void)
{
	// Disable watchdog if enabled by bootloader/fuses 
	MCUSR &= ~(1 << WDRF);
	wdt_disable();

	// Disable Clock Division
	SetSystemClockPrescaler(0);

	// Hardware Initialization
	DDRD = _BV(PIN4); // PIN4 is output, all others are input
	PORTD = 0xFF; // Turn PIN4 'ON' and enable pullup for inputs
			
	OCR0A = 99; // count 100 pulses [0-99] (=100uS) before overflow interrupt
	TCCR0A = 0x02;  // CTC counter mode, OCx disabled
	TCCR0B = 0x02;  // timer clock = sys clock/8, 8Mhz = 1uS per timer pulse 
	TIMSK0 = 0x02;  // enable output compare A interrupt

	TCCR1A = 0x00;
	TCCR1B = 0x03; // timer 1 clock = sys clock/64, overflow 524288uS (about 0.5 sec)
	TIMSK1 = 0x01; // enable timer overflow interrupt

	EICRA = 0x55; // External interrupts 0-3 on any edge
	EICRB = 0x11; // External interrupts 4,6 on any edge
	//EIMSK = 0x0F; // Enable interrupts for ext 0-3
	
	// Initialize Scheduler so that it can be used 
	Scheduler_Init();

	// Initialize USB Subsystem
	USB_Init();

	for (int ii = 0; ii < TIMER_COUNT; ii++)
	{
		Timers[ii].sendTime = false;
		Timers[ii].lasttime = 0;
		Timers[ii].timestring[0] = 0xEE;
		Timers[ii].timestring[1] = ii;
		Timers[ii].timestring[2] = 0;
		Timers[ii].timestring[3] = 0;
		Timers[ii].timestring[4] = 0;
		Timers[ii].timestring[5] = 0;
		Timers[ii].timestring[6] = '\r';
		Timers[ii].timestring[7] = '\n';
	}

	Update.flag = false;
	Update.time = 0;
	Update.string[0] = 0xEE;
	Update.string[1] = 99,
	Update.string[2] = 0;
	Update.string[3] = 0;
	Update.string[4] = 0;
	Update.string[5] = 0;
	Update.string[6] = '\r';
	Update.string[7] = '\n';

	Timers[0].mask = 0x01; // INT0
	Timers[1].mask = 0x02; // INT1
	Timers[2].mask = 0x04; // INT2
	Timers[3].mask = 0x08; // INT3
#if TIMER_COUNT > 4
	Timers[4].mask = 0x10; // INT4
#if TIMER_COUNT > 5
	Timers[5].mask = 0x40; // INT6
#endif
#endif

	// Scheduling - routine never returns, so put this last in the main function
	Scheduler_Start();
}


EVENT_HANDLER(USB_Connect)
{
	// Start USB management task 
	Scheduler_SetTaskMode(USB_USBTask, TASK_RUN);
}

EVENT_HANDLER(USB_Disconnect)
{
	// Stop running CDC and USB management tasks
	Scheduler_SetTaskMode(Timer_Task, TASK_STOP);
	Scheduler_SetTaskMode(USB_USBTask, TASK_STOP);
}

EVENT_HANDLER(USB_ConfigurationChanged)
{
	// Setup CDC Notification, Rx and Tx Endpoints 
	Endpoint_ConfigureEndpoint(CDC_NOTIFICATION_EPNUM, EP_TYPE_INTERRUPT,
		                       ENDPOINT_DIR_IN, CDC_NOTIFICATION_EPSIZE,
	                           ENDPOINT_BANK_SINGLE);

	Endpoint_ConfigureEndpoint(CDC_TX_EPNUM, EP_TYPE_BULK,
		                       ENDPOINT_DIR_IN, CDC_TXRX_EPSIZE,
	                           ENDPOINT_BANK_SINGLE);

	Endpoint_ConfigureEndpoint(CDC_RX_EPNUM, EP_TYPE_BULK,
		                       ENDPOINT_DIR_OUT, CDC_TXRX_EPSIZE,
	                           ENDPOINT_BANK_SINGLE);

	// Start CDC task
	Scheduler_SetTaskMode(Timer_Task, TASK_RUN);
}

EVENT_HANDLER(USB_UnhandledControlPacket)
{
	uint8_t* LineCodingData = (uint8_t*)&LineCoding;

	// Process CDC specific control requests 
	switch (bRequest)
	{
		case GET_LINE_CODING:
			if (bmRequestType == (REQDIR_DEVICETOHOST | REQTYPE_CLASS | REQREC_INTERFACE))
			{	
				// Acknowedge the SETUP packet, ready for data transfer 
				Endpoint_ClearSetupReceived();

				// Write the line coding data to the control endpoint 
				Endpoint_Write_Control_Stream_LE(LineCodingData, sizeof(CDC_Line_Coding_t));
				
				// Send the line coding data to the host and clear the control endpoint 
				Endpoint_ClearSetupOUT();
			}
			
			break;
		case SET_LINE_CODING:
			if (bmRequestType == (REQDIR_HOSTTODEVICE | REQTYPE_CLASS | REQREC_INTERFACE))
			{
				// Acknowedge the SETUP packet, ready for data transfer 
				Endpoint_ClearSetupReceived();

				// Read the line coding data in from the host into the global struct 
				Endpoint_Read_Control_Stream_LE(LineCodingData, sizeof(CDC_Line_Coding_t));

				// Send the line coding data to the host and clear the control endpoint 
				Endpoint_ClearSetupIN();
			}
	
			break;
		case SET_CONTROL_LINE_STATE:
			if (bmRequestType == (REQDIR_HOSTTODEVICE | REQTYPE_CLASS | REQREC_INTERFACE))
			{
				// Acknowedge the SETUP packet, ready for data transfer 
				Endpoint_ClearSetupReceived();
				
				// Send an empty packet to acknowedge the command (currently unused) 
				Endpoint_ClearSetupIN();
			}
	
			break;
	}
}

TASK(Timer_Task)
{
	bool wrote = false;

	// Select the Serial Tx Endpoint 
	Endpoint_SelectEndpoint(CDC_TX_EPNUM);
	for (int ii = 0; ii < TIMER_COUNT; ii++)
	{
		if (Timers[ii].sendTime)
		{
			Endpoint_Write_Stream_LE(Timers[ii].timestring, sizeof(Timers[ii].timestring));
			Timers[ii].sendTime = false;
			wrote = true;
		}
	}

	if (Update.flag)
	{
		Endpoint_Write_Stream_LE(Update.string, sizeof(Update.string));
		Update.flag = false;
		wrote = true;
	}
		
	if (wrote)
		Endpoint_ClearCurrentBank();

	// Select the Serial Rx Endpoint and throw away any recieved data from the host
	Endpoint_SelectEndpoint(CDC_RX_EPNUM);
	if (Endpoint_ReadWriteAllowed())
	  Endpoint_ClearCurrentBank();
}

