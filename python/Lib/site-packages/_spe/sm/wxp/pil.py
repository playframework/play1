import wx
import Image             # Only if you need and use the PIL library.

def bitmap2pil(bitmap):
    return imageToPil(bitmap2image(bitmap))

def bitmap2image(bitmap):
    return wx.ImageFromBitmap(bitmap)


def pil2bitmap(pil):
    return image2bitmap(pil2image(pil))

def pil2image(pil):
    image = wx.EmptyImage(pil.size[0], pil.size[1])
    image.SetData(pil.convert('RGB').tostring())
    return image
    
def pil2dc(pil):
    dc = wx.MemoryDC()
    dc.SelectObject(pil2bitmap(pil))
    return dc

def image2pil(image):
    pil = Image.new('RGB', (image.GetWidth(), image.GetHeight()))
    pil.fromstring(image.GetData())
    return pil

def image2bitmap(image):
    return image.ConvertToBitmap()
    
## Conversions among wxImage, wxBitmap, wxCursor, wxIcon and DATA
##      wxImage to wxBitmap --  myWxImage.ConvertToBitmap() or wxBitmapFromImage(myWxImage) 
##      wxImage to DATA --  myWxImage.GetData()  returning a string in width * height * 3 format
##      DATA to wxImage --  image = wxImage(); image.SetData( data )  where data is a Python string of length width * height * 3.
##      DATA to wxBitmap -- Go through wxImage to get to wxBitmap.
##      DATA to wxIcon -- Should be possible, but I don't see an overloaded-constructor name for it.
##      wxIcon to wxBitmap --  bitmap = wxEmptyBitmap( icon.GetWidth(), icon.GetHeight()); bitmap.CopyFromIcon( icon )  