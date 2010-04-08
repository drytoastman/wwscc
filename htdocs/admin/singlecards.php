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

	## 
	# 5x8 Card is 360x576 and we add a margin all around of 20
    ##
	function DrawCard($event, $entrant, $imagename, $imagesize)
	{
		$dimA = 50;
		$dimB = 210;
		$dimC = 40;
		$dimD = 100;
		$dimE = 70;
		$dimF = 66;

		$height = 21;


		# BarCode - top right
		if (!empty($entrant->carid))
		{
			$this->SetFont('Arial','',8);
			$this->Code39Right(556, $this->GetY(), $entrant->carid, 20);
			$this->Text(556 - $this->GetStringWidth($entrant->carid), $this->GetY() + 27, $entrant->carid);
		}

		# Card Image - top left
		if (is_readable($imagename) && !is_dir($imagename))
		{
			#$this->Image($imagename, 288 - ($imagesize[0]/2), 300 - $imagesize[1]);
			$this->Image($imagename, 15, 18);
		}

		# The event title centered
		$this->SetFont('Arial','B',12);
		$this->Cell(0, 30, $event->label(), 0, 0, 'C');


		# Move down and start the standard size cells
		$this->SetFont('Arial','',10);
		$this->NextLine(30);
	
		$this->Label($dimA, $height, "Name:");
		$this->Value($dimB, $height, $entrant->fullname());
		$this->Label($dimC, $height, "Year:");
		$this->Value($dimD, $height, $entrant->year);
		$this->Label($dimE, $height, "Class(Index):");
		$this->Value($dimF, $height, $entrant->clsstring());
		$this->NextLine($height);
	
		$this->Label($dimA, $height, "Address:");
		$this->Value($dimB, $height, $entrant->address);
		$this->Label($dimC, $height, "Make:");
		$this->Value($dimD, $height, $entrant->make);
		$this->Label($dimE, $height, "Number:");
		$this->Value($dimF, $height, $entrant->number);
		$this->NextLine($height);
	
		$this->Label($dimA, $height, "CSZ:"); 
		$this->Value($dimB, $height, $entrant->city." ".$entrant->state." ".$entrant->zip);
		$this->Label($dimC, $height, "Model:");
		$this->Value($dimD, $height, $entrant->model);
	
		$this->NextLine(10);
		$this->SetX(470);
		$this->Cell(30, $height, "Tech", 0, 0, 'L');
		$this->Cell(30, $height, "", 1, 0, 'L');
		$this->NextLine(11);
	
		$this->Label($dimA, $height, "Phone:");
		$this->Value($dimB, $height, $entrant->homephone);
		$this->Label($dimC, $height, "Color:");
		$this->Value($dimD, $height, $entrant->color);
		$this->NextLine($height);
	
		$this->Label($dimA, $height, "Email:");
		$this->Value($dimB, $height, $entrant->email);
		$this->Label($dimC, $height, "Memb:");
		$this->Value($dimD+$dimE+$dimF, $height, $entrant->membernumber);
		$this->NextLine($height);
	
		$this->Label($dimA, $height, "Sponsor:");
		$this->Value($dimB, $height, $entrant->sponsor);
		$this->Label($dimC, $height, "Brag:");
		$this->Value($dimD+$dimE+$dimF, $height, $entrant->brag);
		$this->NextLine($height);

		$this->NextLine(6);

		$height = 16;
		$this->Cell(210, $height, "Scratch",	1, 0, 'C');
		$this->Cell(50, $height, "Cones",	1, 0, 'C');
		$this->Cell(50, $height, "Gates",	1, 0, 'C');
		$this->Cell(0, $height, "Total",	1, 1, 'C');

		$height = 23;
		for ($ii = 1; $ii <= 6; $ii++)
		{
			$this->Cell(210, $height, "$ii.",1, 0, 'L');
			$this->Cell(50, $height, "",	1, 0, 'L');
			$this->Cell(50, $height, "",	1, 0, 'L');
			$this->Cell(0, $height, "",	1, 1, 'L');
		}
	}
	

	function Code39Right($xpos, $ypos, $code, $height=20, $baseline=3)
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
		for ($ii = strlen($code)-1; $ii >= 0; $ii--)
		{
			$seq = $barChar[$code[$ii]];
			if (!isset($seq))
				continue;


			for($bar = 8; $bar >= 0; $bar--)
			{
				if ($seq{$bar} == 'n')
					$lineWidth = $narrow;
				else
					$lineWidth = $wide;

				if ($bar % 2 == 0)
					$this->SetFillColor(0);
				else
					$this->SetFillColor(255);
				
				$xpos -= $lineWidth;
				$this->Rect($xpos, $ypos, $lineWidth, $height, 'F');
			}
			$xpos -= $gap;
		}
	}
}


function printCards($event, $entries)
{
	$cardimage = "images/" . loadSetting('register_cardimage');
	if (is_readable($cardimage) && !is_dir($cardimage))
		$imagesize = getimagesize($cardimage);

	$pdf = new TimeCardList('P', 'pt', array(576,360));  # 8"x5"
	$pdf->SetLineWidth(.3);
	$pdf->SetAutoPageBreak(false);
	$pdf->SetMargins(20, 20);

	if ($entries == null) // blank card pair
	{
		$nobody = new RegEntry();
		$pdf->AddPage(); 
		$pdf->DrawCard($event, $nobody, $cardimage, $imagesize);
		$pdfname = strtolower($event->name . "blank.pdf");
		$pdf->Output($pdfname, 'D');
		return;
	}

	foreach ($entries as $reg)
	{
		$pdf->AddPage(); 
		$pdf->DrawCard($event, $reg, $cardimage, $imagesize);
	}
	
	$pdfname = strtolower($event->name . ".pdf");
	$pdf->Output($pdfname, 'D');
}

?>
