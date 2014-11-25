To transform an edited BMP back to BM6 format, you simply have to use the program bmp_tiler.jar with the following command:

java -jar bmp_tiler.jar <edited_bmp> <BM6_file>

For example:

java -jar bmp_tiler.jar 0518_edited.bmp 0518.BM6


Things to keep in mind:
- The BMP file has to be indexed and use only 16 colours (4bpp).
- Both the width and height of the BMP file must be a multiple of 8 (becaue tiles inside a BM6 are 8x8).