<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



require_once('fpdf/fpdf.php');

function printNameList($name, $title, $personlist)
{
	$pdf = new FPDF('P', 'pt', 'letter');
	$pdf->SetLineWidth(.3);
	$pdf->SetAutoPageBreak(true, 15);
	$pdf->AddPage();

	$pdf->SetFont('Arial','B',18);
	$pdf->Write(16, $title . " (" . count($personlist) . " people)\n\n"); 
	$pdf->SetFont('Arial','',10);

	$cols = 4;
	$count = count($personlist);
	$height = ceil($count/$cols);

	for ($ii = 0; $ii < $height; $ii++)
	{
		for ($jj = $ii; $jj < $count; $jj += $height)
		{
			$p = $personlist[$jj];
			$pdf->Cell(140, 12, "{$p->lastname}, {$p->firstname}");
		}
		$pdf->Write(10, "\n");
	}
	$pdf->Write(10, "\n");
	$pdf->Output("$name.pdf", 'D');
}


function printNumbers($classlist)
{
	$pdf = new FPDF('P', 'pt', 'letter');
	$pdf->SetLineWidth(.3);
	$pdf->SetAutoPageBreak(true, 15);
	$pdf->AddPage();

	$pdf->SetFont('Arial','B',18);
	$pdf->Write(16, "Number Report - " . date("M d Y") . "\n\n"); 
	foreach ($classlist as $c => $cars)
	{
		$count = count($cars);
		$height = ceil($count/3);
		if ($count <= 0)
			continue;

		$pdf->SetFont('Arial','B',15);
		$pdf->Write(16, "$c\n"); 
		$pdf->SetFont('Arial','',10);

		for ($ii = 0; $ii < $height; $ii++)
		{
			for ($jj = $ii; $jj < $count; $jj += $height)
			{
				$car = $cars[$jj];
				$pdf->Cell(200, 12, "{$car->number} - {$car->firstname} {$car->lastname}");
			}
			$pdf->Write(10, "\n");
		}
		$pdf->Write(10, "\n");
	}
	
	$pdf->Output("numbers.pdf", 'D');
}

function printPayPalList($event, $payments)
{
	$pdf = new FPDF('P', 'pt', 'letter');
	$pdf->SetLineWidth(.3);
	$pdf->SetAutoPageBreak(true, 15);
	$pdf->AddPage();

	$pdf->SetFont('Arial','B',18);
	$pdf->Write(16, "PayPal List for {$event->name}\n\n");

	$cols = 4;
	$count = count($payments);
	$height = ceil($count/$cols);

	$pdf->SetFont('Arial','B',10);
	$pdf->Cell(120, 12, "TxId"); 
	$pdf->Cell(120, 12, "Name");
	$pdf->Cell(50, 12, "Value");
	$pdf->cell(50, 12, "Type");
	$pdf->cell(50, 12, "Status");
	$pdf->Write(12, "\n");

	$pdf->SetFont('Arial','',10);
	foreach ($payments as $p)
	{
		$pdf->Cell(120, 12, $p->txid);
		$pdf->Cell(120, 12, "{$p->lastname}, {$p->firstname}");
		$pdf->Cell(50, 12, $p->amount);
		$pdf->cell(50, 12, $p->type);
		$pdf->cell(50, 12, $p->status);
		$pdf->Write(11, "\n");
	}

	$pdf->Output("paypal.pdf", 'D');
}

?>
