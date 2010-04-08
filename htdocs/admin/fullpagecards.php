<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



require_once('fpdf/fpdf.php');

class TimeCardList extends FPDF
{
	function Label($width, $height, $str)
	{
		$this->SetFont('Arial','B',10);
		$this->Cell($width, $height, $str, 'TLB', 0, 'R');
	}

	function Value($width, $height, $str)
	{
		$this->SetFont('Arial','',10);
		$this->Cell($width, $height, $str, 'TRB', 0, 'L');
	}

	function NextLine($height)
	{
		$this->SetY($this->GetY() + $height);
	}

	function Cell($w,$h=0,$txt='',$border=0,$ln=0,$align='',$fill=0,$link='')
	{
		if ($w != 0)
		{
			$cw=&$this->CurrentFont['cw'];
			$wmax=($w-2*$this->cMargin)*1000/$this->FontSize;
			$len = strlen($txt);
			$sw = 0;
			for ($ii = 0; $ii < $len; $ii++)
			{
				$sw += $cw[$txt[$ii]];
				if ($sw > $wmax)
				{
					$txt = substr($txt, 0, $ii);
					break;
				}
			}
		}

		FPDF::Cell($w, $h, $txt, $border, $ln, $align, $fill, $link);
	}

	function DrawCard($event, $entrant, $imagename, $imagesize)
	{
		$labelLwidth = 50;
		$valueLwidth = 226;
		$labelRwidth = 40;
		$valueRwidth = 100;
		$height = 22;
	
		$this->SetFont('Arial','B',12);
		$this->Cell(0, 30, $event->label(), 0, 0, 'C');
	
		$this->SetFont('Arial','',8);
		if (!empty($entrant->carid))
			$this->Code39(450, $this->GetY(), $entrant->carid, 20);

		if (is_readable($imagename) && !is_dir($imagename))
		{
			$this->Image($imagename, $this);
		}

		$this->Text(520, $this->GetY() + 27, "{$entrant->carid}-1");
	
		# Move down and start the standard size cells
		$this->SetFont('Arial','',10);
		$this->SetY($this->GetY() + 30);
	
		$this->Label($labelLwidth, $height, "Name:");
		$this->Value($valueLwidth, $height, $entrant->fullname());
		$this->Label($labelRwidth, $height, "Year:");
		$this->Value($valueRwidth, $height, $entrant->year);
		$this->Label(70, $height, "Class(Index):");
		$this->Value( 0, $height, $entrant->clsstring());
		$this->NextLine($height);
	
		$this->Label($labelLwidth, $height, "Address:");
		$this->Value($valueLwidth, $height, $entrant->address);
		$this->Label($labelRwidth, $height, "Make:");
		$this->Value($valueRwidth, $height, $entrant->make);
		$this->Label(70, $height, "Number:");
		$this->Value( 0, $height, $entrant->number);
		$this->NextLine($height);
	
		$this->Label($labelLwidth, $height, "CSZ:"); 
		$this->Value($valueLwidth, $height, $entrant->city." ".$entrant->state." ".$entrant->zip);
		$this->Label($labelRwidth, $height, "Model:");
		$this->Value($valueRwidth, $height, $entrant->model);
	
		$this->SetY($this->GetY() + 11);
		$this->SetX(470);
		$this->Cell(30, $height, "Tech", 0, 0, 'L');
		$this->Cell(30, $height, "", 1, 0, 'L');
		$this->SetY($this->GetY() + 11);
	
		$this->Label($labelLwidth, $height, "Phone:");
		$this->Value($valueLwidth, $height, $entrant->homephone);
		$this->Label($labelRwidth, $height, "Color:");
		$this->Value($valueRwidth, $height, $entrant->color);
		$this->NextLine($height);
	
		$this->Label($labelLwidth, $height, "Email:");
		$this->Value($valueLwidth, $height, $entrant->email);
		$this->Label($labelRwidth, $height, "Clubs:");
		$this->Value(           0, $height, $entrant->clubs);
		$this->NextLine($height);
	
		$this->Label($labelLwidth, $height, "Sponsor:");
		$this->Value($valueLwidth, $height, $entrant->sponsor);
		$this->Label($labelRwidth, $height, "Brag:");
		$this->Value(           0, $height, $entrant->brag);
		$this->NextLine($height + 8);

		$height = 20;
		$this->Cell(226, $height, "Scratch",	1, 0, 'C');
		$this->Cell(50, $height, "Cones",	1, 0, 'C');
		$this->Cell(50, $height, "Gates",	1, 0, 'C');
		$this->Cell(0, $height, "Total",	1, 1, 'C');

		$height = 24;
		for ($ii = 1; $ii <= 6; $ii++)
		{
			$this->Cell(226, $height, "$ii.",1, 0, 'L');
			$this->Cell(50, $height, "",	1, 0, 'L');
			$this->Cell(50, $height, "",	1, 0, 'L');
			$this->Cell(0, $height, "",	1, 1, 'L');
		}
	}
	
	function Code39($xpos, $ypos, $code, $height=20, $baseline=3)
	{
		$wide = $baseline;
		$narrow = $baseline / 3 ;
		$gap = $narrow;
	
		$barChar['0'] = 'nnnwwnwnn';
		$barChar['1'] = 'wnnwnnnnw';
		$barChar['2'] = 'nnwwnnnnw';
		$barChar['3'] = 'wnwwnnnnn';
		$barChar['4'] = 'nnnwwnnnw';
		$barChar['5'] = 'wnnwwnnnn';
		$barChar['6'] = 'nnwwwnnnn';
		$barChar['7'] = 'nnnwnnwnw';
		$barChar['8'] = 'wnnwnnwnn';
		$barChar['9'] = 'nnwwnnwnn';
		$barChar['*'] = 'nwnnwnwnn';
		$barChar['-'] = 'nwnnnnwnw';

		$this->SetFillColor(0);

		$code = '*'.strtoupper($code).'*';
		for ($i = 0; $i < strlen($code); $i++)
		{
			$char = $code{$i};
			if (!isset($barChar[$char]))
			{
				continue;
				//$this->Error('Invalid character in barcode: '.$char);
			}

			$seq = $barChar[$char];
			for($bar=0; $bar<9; $bar++)
			{
				if ($seq{$bar} == 'n')
					$lineWidth = $narrow;
				else
					$lineWidth = $wide;

				if ($bar % 2 == 0)
					$this->SetFillColor(0);
				else
					$this->SetFillColor(255);
				
				$this->Rect($xpos, $ypos, $lineWidth, $height, 'F');
				$xpos += $lineWidth;
			}
			$xpos += $gap;
		}
	}
}


function printCards($event, $entries)
{
	$cardimage = "images/" . loadSetting('admin_cardimage');
	if (is_readable($cardimage) && !is_dir($cardimage))
		$imagesize = getimagesize($cardimage);


	$pdf = new TimeCardList('P', 'pt', 'letter');
	$pdf->SetLineWidth(.3);
	$pdf->SetAutoPageBreak(false);

	if ($entries == null) // blank card pair
	{
		$nobody = new RegEntry();
		$pdf->AddPage(); 
		$pdf->DrawCard($event, $nobody, $cardimage, $imagesize);
		$pdf->SetY(400); 
		$pdf->DrawCard($event, $nobody, $cardimage, $imagesize);
		$pdfname = strtolower($event->name . "blank.pdf");
		$pdf->Output($pdfname, 'D');
		return;
	}

	$ii = 0;
	foreach ($entries as $reg)
	{
		if ($ii % 2) // Space from bottom of previous card
		{
			$pdf->SetY(400); 
		}
		else // Create new page and space from top
		{
			$pdf->AddPage(); 
		}
	
		$pdf->DrawCard($event, $reg, $cardimage, $imagesize);
		$ii++;
	}
	
	$pdfname = strtolower($event->name . ".pdf");
	$pdf->Output($pdfname, 'D');
}

?>
