#!/usr/bin/python

import os, io, logging
log = logging.getLogger(__name__)

class Bracket(object):

    RANK1 =  [ 1 ]
    RANK2 =  [ 1, 2 ]
    RANK4 =  [ 1, 4, 2, 3 ]
    RANK8 =  [ 1, 8, 4, 5, 2, 7, 3, 6 ]
    RANK16 = [ 1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11 ]
    RANK32 = [ 1, 32, 16, 17, 8, 25, 9, 24, 4, 29, 13, 20, 5, 28, 12, 21, 2, 31, 15, 18, 7, 26, 10, 23, 3, 30, 14, 19, 6, 27, 11, 22 ]
    RANKS = RANK32 + RANK16 + RANK8 + RANK4 + RANK2 + RANK1 + [0]
    RANKS.reverse()

    def __init__(self, depth, rounds=None, draw=True):
        self.roundwidth = 145
        self.depth = depth
        self.rounds = rounds
        self.coords = list()

        self.baserounds = int(2**(depth-1))
        self.imagesize, self.initialspacing = {
                2: ((470, 270), 44), 
                4: ((615, 400), 44), 
                8: ((760, 530), 33), 
                16: ((905,710), 22), 
            }.get(self.baserounds, None)
    
        if self.imagesize is None:
            log.error("drawBracket with invalid sizing %d" % (depth))
            return
    
        if not draw: # When calculating coords we don't do any drawing
            self.draw = None
            return 

        try:
            from PIL import Image, ImageFont, ImageDraw
        except:
            import Image, ImageFont, ImageDraw
        
        self.image = Image.new("L", self.imagesize, "White")
        self.draw = ImageDraw.Draw(self.image)
        self.font = ImageFont.truetype(os.path.join(os.path.dirname(__file__), 'universal.ttf'), 11)
        self.fill = 'black'


    @classmethod
    def coords(cls, depth):
        b = Bracket(depth, draw=False)
        b.drawTree()
        return b.coords, b.imagesize

    @classmethod
    def image(cls, depth, rounds):
        return Bracket(depth, rounds).drawTree()


    def doEntrant(self, x, y, e):
        dialtxt = "%0.3lf" % e.dial
        w2, h2 = self.font.getsize(dialtxt)
        name = e.firstname + " " + e.lastname
        w1, h1 = self.font.getsize(name)
        while w1 > 100:
            name = name[:-1]
            w1, h1 = self.font.getsize(name)
        
        self.draw.text((x+4, y-h1-1), name, font=self.font, fill=self.fill)
        self.draw.text((x+self.roundwidth-w2-4, y-h2), dialtxt, font=self.font, fill=self.fill)

    def line(self, x1, y1, x2, y2):
        if self.draw is None: return
        self.draw.line(((x1,y1), (x2,y2)), fill='black')

    def drawBracket(self, x, y, spacing, rnd):
        self.coords.append((rnd, "%d,%d,%d,%d" % (x, y-10, x+self.roundwidth, y+spacing)))
        if self.draw is None: return

        self.doEntrant(x, y, self.rounds[rnd].e1)
        self.line(x, y, x+self.roundwidth, y)
        y += spacing
        self.doEntrant(x, y, self.rounds[rnd].e2)
        self.line(x, y, x+self.roundwidth, y)
        x += self.roundwidth
        self.line(x, y-spacing, x, y)

    def drawTree(self):
        self.coords = list()
        startx = 13
        starty = 20
        spacing = self.initialspacing
        x = 0
        y = 0
        
        # Draw each round of brackets 
        ii = self.baserounds
        rnd = (self.baserounds*2)-1
        while ii > 0:
            # Line ourselves up 
            x = startx
            y = starty
            
            # Draw one vertical line of brackets 
            for jj in range(0, ii):
                # Draw first horizontal, second horizontal and then right hand vertical 
                if self.draw is not None and ii == self.baserounds:
                    w2, h2 = self.font.getsize("32")
                    self.draw.text((x-12, y-h2), str(self.RANKS[rnd*2+1]), font=self.font, fill=self.fill)
                    self.draw.text((x-12, y+spacing-h2), str(self.RANKS[rnd*2]), font=self.font, fill=self.fill)
                self.drawBracket(x, y, spacing, rnd)
                y += (2*spacing)
                rnd -= 1
            
            # Adjust our starting position and spacing for the next column 
            startx += self.roundwidth
            starty += spacing/2
            spacing *= 2
            ii = int(ii / 2)
    
        # Draw the third place bracket and 3rd place winner line 
        x += 20
        y = y - (spacing/2) + (2*self.initialspacing)
        self.drawBracket(x, y, self.initialspacing, 99)

        # Coords are complete, if we aren't drawing we can skip out here
        if self.draw is None:
            return None

        # Draw the 3rd and 1st place winners (round 0)
        x += self.roundwidth;
        y += (self.initialspacing/2);
        self.doEntrant(x, y, self.rounds[0].e2)
        self.line(x, y, x+self.roundwidth-5, y)
        self.doEntrant(startx, starty, self.rounds[0].e1)
        self.line(startx, starty, startx+self.roundwidth-5, starty)
    
        with io.BytesIO() as f:
            self.image.save(f, "PNG")
            return f.getvalue()

        return "Error"

