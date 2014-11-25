/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package srwgcbmptiler;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Jonatan
 */
public class Main {

    static int num_colours = 0;
    static int width = 0;
    static int height = 0;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        if (args.length != 2){
            System.out.println("ERROR: Wrong number of parameters: " + args.length);
            System.out.println("USE:\n java -jar bmp_tiler <bmp_file> <result>");
            return;
        }

        if (!args[0].endsWith(".bmp") && !args[0].endsWith(".BMP")){
            System.out.println("ERROR: Tried to read a not-BMP file" + args.length);
            return;
        }

        byte[] data = getTiledBytes(args[0]);

        if (data != null)
            saveTiledBMP(args[1], data);
        else
            System.err.println("ERROR: Tried to process a wrong file!!");
    }

    public static byte[] getTiledBytes(String filename){
        byte[] data = null;

        try{
            RandomAccessFile f = new RandomAccessFile(filename, "r");

            byte[] header = new byte[54];

            f.read(header);

            if (header[0] != 'B' || header[1] != 'M')
                return null;

            byte[] aux = new byte[4];

            // Get the width
            aux[3] = header[18];
            aux[2] = header[19];
            aux[1] = header[20];
            aux[0] = header[21];

            width = byteSeqToInt(aux);

            System.out.println("Width: " + width);

            // Get the height
            aux[3] = header[22];
            aux[2] = header[23];
            aux[1] = header[24];
            aux[0] = header[25];

            height = byteSeqToInt(aux);

            System.out.println("Height: " + height);

            // Get the number of colours
            aux[3] = header[50];
            aux[2] = header[51];
            aux[1] = header[52];
            aux[0] = header[53];

            num_colours = byteSeqToInt(aux);

            System.out.println("Number of colours: " + num_colours);

            // FORCE 16 COLOURS!!
            /*if (num_colours != 16){
                System.out.println("Screw that. We use 16 colours.");
                num_colours = 16;
            }*/

            if (num_colours == 0){
                System.err.println("ERROR: Not an indexed BMP!!");
                return null;
            }

            // Skip the palette data
            int BGRs = 0;

            f.read(aux);

            if (aux[0] == 'B' && aux[1] == 'G' && aux[2] == 'R')
                BGRs = 64;

            f.seek(54 + (num_colours * 4) + BGRs);

            // Get the data
            int size = 0;

            if (num_colours == 16)
                size = (width * height) / 2;    // 4bpp - 1 byte is 2 colours
            else if (num_colours == 256)
                size = width * height;  // 8 bpp - 1 byte is 1 colour

            aux = new byte[size];

            f.read(aux);

            f.close();

            // Process the data
            //byte[] predata = new byte[size];
            data = new byte[size];

            int tile_size = 64;  // Tiles are 8x8 pixels. The size depends on the bpp

            int tile_row_size = 8;  // bytes in a row
            int tile_col_size = 8;

            if (num_colours == 16){  // 4bpp
                tile_size = 32;
                tile_col_size = 4;
            }

            int x = 0;
            int y = 0;
            //int counter = 0;

            int tiles_per_row = width / tile_col_size;
            if (num_colours == 16)
                tiles_per_row /= 2;


            byte[] pixels_R = aux.clone();
            int dimX = (width / 8) * tile_col_size;

            for (int i = 0, j = aux.length - dimX; i < aux.length; i+=dimX, j-=dimX){
                for (int k = 0; k < dimX; ++k){
                    //System.out.println("Length: " + pixels.length + " i: " + i + " j: " + j + " k: " + k);
                    aux[i + k] = pixels_R[j + k];
                }
            }
            // Save an upside down version of the original image data and work with that
            // That way we avoid getting the tiles flipped later
            /*for (int i = 0; i < size; i++){
                predata[size - 1 - i] = aux[i];
            }*/
            /*for (int i = 0, j = predata.length - width; i < predata.length; i+=width, j-=width){
                for (int k = 0; k < width; ++k){
                    //System.out.println("Length: " + pixels.length + " i: " + i + " j: " + j + " k: " + k);
                    // Reverse the nybbles
                    byte value = aux[j + k];
                    byte low = (byte) (value & 0x0f);
                    byte high = (byte) (value & 0xf0);
                    byte reversed = (byte) ((low << 4) + (high >> 4));
                    //predata[i + k] = aux[j + k];
                    predata[i + k] = reversed;
                }
            }*/

            // This stores the data UPSIDE-DOWN!!!!!
            for (int i = 0; i < size; i+= tile_size){
                int col = 0;
                int row = 0;
                int pos = 0;
                
                // We need to determine where is the start of the tile
                int start = 0;
                int row_width = width;
                if (num_colours == 16)
                    row_width /= 2;   // Divide by 2 because it's 4bpp
                
                // We have to determine how many rows of tiles we've gone past (y)
                // and how many columns of tiles we've gone past (x)
                start = (x * tile_col_size) + (y * tile_row_size * row_width );

                //System.out.println("Tile: " + counter + " i: " + i + " x: " + x + " y: " + y +" start: " + start);
            
                for (int j = 0; j < tile_size; j++){
                    pos = start + ( row_width * row ) + col;

                    //System.out.println((i+j) + ": position " + pos);

                    data[i + j] = aux[pos];
                    //data[i + j] = predata[pos];

                    col++;
                    if (col == tile_col_size){
                        col = 0;
                        row++;
                    }
                }

                x++;
                if (x == tiles_per_row){
                    x = 0;
                    y++;
                }

                //counter++;
            }


        } catch (IOException ex){
            System.err.println("ERROR: Couldn't read edited BMP!!");
        }


        return data;
    }


    public static void saveTiledBMP(String filename, byte[] content){
        try{
            RandomAccessFile f = new RandomAccessFile(filename, "rw");

            // Prepare the header
            byte[] header = new byte[32];

            header[0] = 'B';
            header[1] = 'M';
            header[2] = 'P';

            if (num_colours == 16)
                header[3] = 6;
            else if (num_colours == 256)
                header[3] = 9;

            byte[] aux = intToByteSeq(num_colours);
            //byte[] aux = intToByteSeq(16);

            header[4] = aux[3];
            header[5] = aux[2];
            header[6] = aux[1];
            header[7] = aux[0];

            aux = intToByteSeq(width);

            header[8] = aux[3];
            header[9] = aux[2];
            header[10] = aux[1];
            header[11] = aux[0];

            aux = intToByteSeq(height);

            header[12] = aux[3];
            header[13] = aux[2];
            header[14] = aux[1];
            header[15] = aux[0];

            // Write the header
            f.write(header);

            // Write the data
            f.write(content);

            f.close();
        } catch (IOException ex){
            System.err.println("ERROR: Couldn't write file!!");
        }
    }


    // Takes a 4-byte hex little endian and returns its int value
    // BMP files use little endianness
    public static int byteSeqToInt(byte[] byteSequence){
        if (byteSequence.length != 4)
            return -1;

        int value = 0;
        value += byteSequence[3] & 0xff;
        value += (byteSequence[2] & 0xff) << 8;
        value += (byteSequence[1] & 0xff) << 16;
        value += (byteSequence[0] & 0xff) << 24;
        return value;
    }


    // Takes an int and return its big endian 4-byte value
    // BM files use big endianness
    public static byte[] intToByteSeq(int value){
        byte[] byteSequence = new byte[4];

        byteSequence[0] = (byte) (value & 0xff);
        byteSequence[1] = (byte) ( (value >> 8) & 0xff  );
        byteSequence[2] = (byte) ( (value >> 16) & 0xff  );
        byteSequence[3] = (byte) ( (value >> 24) & 0xff  );

        return byteSequence;
    }

}
