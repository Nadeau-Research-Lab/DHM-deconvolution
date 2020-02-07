package edu.pdx.imagej.deconv;

import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.plugin.HyperStackConverter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.jtransforms.fft.FloatFFT_3D;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Deconvolve_Image_Utils {
    
    // returns (a + b*i) = (c + d*i)/(e + f*i) as {a, b}
    static private float[] complexDivide(float c, float d, float e, float f) {
        double denom = Math.pow(e, 2) + Math.pow(f, 2);
        float re = (float)((c*e + d*f) / denom);
        float im = (float)((d*e - c*f) / denom);
        
        if (!Float.isFinite(re) || !Float.isFinite(im))
            return complexDivideBig(c, d, e, f);
        
        float[] ret = {re, im};
        return ret;
    }
    
    // handle complex division with big numbers
    static private float[] complexDivideBig(float c, float d, float e, float f) {
        try {
            BigDecimal bigC = BigDecimal.valueOf((double)c);
            BigDecimal bigD = BigDecimal.valueOf((double)d);
            BigDecimal bigE = BigDecimal.valueOf((double)e);
            BigDecimal bigF = BigDecimal.valueOf((double)f);
        
            BigDecimal denom = bigE.multiply(bigE).add(bigF.multiply(bigF));
            BigDecimal re = bigC.multiply(bigE).add(bigD.multiply(bigF)).divide(denom, RoundingMode.HALF_UP);
            BigDecimal im = bigD.multiply(bigE).subtract(bigC.multiply(bigF)).divide(denom, RoundingMode.HALF_UP);
        
            float[] ret = {re.floatValue(), im.floatValue()};
            return ret;
        }
        catch (NumberFormatException ex) {
            float[] ret = {0, 0};
            return ret;
        }
    }
    
    // returns (a + b*i) = (c + d*i)*(e + f*i) as {a, b}
    static private float[] complexMult(float c, float d, float e, float f) {
        float re = (c*e - d*f);
        float im = (c*f + d*e);
        
        if (!Float.isFinite(re) || !Float.isFinite(im))
            return complexMultBig(c, d, e, f);
        
        float[] ret = {re, im};
        return ret;
    }
    
    // handle complex multiplication with big numbers
    static private float[] complexMultBig(float c, float d, float e, float f) {
        try {
            BigDecimal bigC = BigDecimal.valueOf((double)c);
            BigDecimal bigD = BigDecimal.valueOf((double)d);
            BigDecimal bigE = BigDecimal.valueOf((double)e);
            BigDecimal bigF = BigDecimal.valueOf((double)f);
        
            BigDecimal re = bigC.multiply(bigE).subtract(bigD.multiply(bigF));
            BigDecimal im = bigC.multiply(bigF).subtract(bigD.multiply(bigE));
        
            float[] ret = {re.floatValue(), im.floatValue()};
            return ret;
        }
        catch(NumberFormatException ex) {
            float[] ret = {0, 0};
            return ret;
        }
    }
    
    // returns (a + b*i) = (c + d*i) - (e + f*i) as {a, b}
    static private float[] complexSub(float c, float d, float e, float f) {
        float re = c - e;
        float im = d - f;
        float[] ret = {re, im};
        
        if (!Float.isFinite(re) || !Float.isFinite(im))
            return complexSubBig(c, d, e, f);
        
        return ret;
    }
    
    // handle complex subtraction with big numbers
    static private float[] complexSubBig(float c, float d, float e, float f) {
        try {
            BigDecimal bigC = BigDecimal.valueOf((double)c);
            BigDecimal bigD = BigDecimal.valueOf((double)d);
            BigDecimal bigE = BigDecimal.valueOf((double)e);
            BigDecimal bigF = BigDecimal.valueOf((double)f);
        
            BigDecimal re = bigC.subtract(bigE);
            BigDecimal im = bigD.subtract(bigF);
        
            float[] ret = {re.floatValue(), im.floatValue()};
            return ret;
        }
        catch(NumberFormatException ex) {
            float[] ret = {0, 0};
            return ret;
        }
    }
    
    // returns (a + b*i) = (c + d*i) + (e + f*i) as {a, b}
    static private float[] complexAdd(float c, float d, float e, float f) {
        float re = c + e;
        float im = d + f;
        float[] ret = {re, im};
        
        if (!Float.isFinite(re) || !Float.isFinite(im))
            return complexAddBig(c, d, e, f);
        
        return ret;
    }
    
    // handle complex addition with big numbers
    static private float[] complexAddBig(float c, float d, float e, float f) {
        try {
            BigDecimal bigC = BigDecimal.valueOf((double)c);
            BigDecimal bigD = BigDecimal.valueOf((double)d);
            BigDecimal bigE = BigDecimal.valueOf((double)e);
            BigDecimal bigF = BigDecimal.valueOf((double)f);
        
            BigDecimal re = bigC.add(bigE);
            BigDecimal im = bigD.add(bigF);
        
            float[] ret = {re.floatValue(), im.floatValue()};
            return ret;
        }
        catch(NumberFormatException ex) {
            float[] ret = {0, 0};
            return ret;
        }
    }
    
    // Show window to select an image file. Returns the file path as a string.
    static public String getPath(String message) {
        String path;
        OpenDialog od = new OpenDialog(message);
        path = od.getPath();

        return path;
    }
    
    // Show window to select a directory. Returns the path as a string.
    static public String getDirectory(String message) {
        String path;
        DirectoryChooser dc = new DirectoryChooser(message);
        path = dc.getDirectory();
    
        return path;
    }
    
    // Takes an image stack and returns a float 3D matrix
    static public float[][][] getMatrix3D(ImagePlus image) {
        float[][][] mat = getMatrix4D(image)[0];
        return mat;
    }
    
    // Takes an image stack and returns a float 4D matrix
    static public float[][][][] getMatrix4D(ImagePlus image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int slices = image.getNSlices();
        int frames = image.getNFrames();
        ImageStack stack = image.getStack();
        float[][][][] mat = new float[frames][slices][height][width];
        
        // loop over image stack and assign values to matrix - note frames, slices, and StackIndex are 1-based
        for (int i = 1; i <= frames; i++ ) {
            for (int j = 1; j <= slices; j++) {
                for (int k = 0; k < height; k++) {
                    for (int l = 0; l < width; l++)
                        mat[i-1][j-1][k][l] = (float)stack.getVoxel(l, k, image.getStackIndex(1, j, i)-1);
                }
            }
        }
        return mat;
    }
    
    // Takes 3D matrix and puts it into a form compatible with the FFT package
    // Even columns are the real parts of data entries, and odd columns are the imaginary parts.    
    // This method assumes a phase of zero, so all the data is real.
    static public float[][][] toFFTform(float[][][] mat) {
        int a_slices = mat.length;
        int a_height = mat[0].length;
        int a_width = mat[0][0].length;
        float[][][] ret = new float[a_slices][a_height][2 * a_width];

        for (int i = 0; i < a_slices; i++) {
            for (int j = 0; j < a_height; j++) {
                for (int k = 0; k < a_width; k++) {
                    // set the real part
                    ret[i][j][2 * k] = mat[i][j][k];
                    ret[i][j][2 * k + 1] = 0;   
                }
            }
        }
        return ret;
    }
    
    static public float[][][][] toFFTform(float[][][][] mat) {
        int a_frames = mat.length;
        int a_slices = mat[0].length;
        int a_height = mat[0][0].length;
        int a_width = mat[0][0][0].length;
        float[][][][] ret = new float[a_frames][a_slices][a_height][2 * a_width];

        for (int i = 0; i < a_frames; i++) {
            ret[i] = toFFTform(mat[i]);
        }
        
        return ret;
    }
    
    // given an amplitude and phase matrix, build a complex matrix compatible with the FFT package
    static public float[][][] toFFTform(float[][][] amp, float[][][] phase) {
        int a_slices = amp.length;
        int a_height = amp[0].length;
        int a_width = amp[0][0].length;
        float[][][] ret = new float[a_slices][a_height][2 * a_width];
        // loop over elements of amp and phase, add appropriate values to ret
        for (int i = 0; i < a_slices; i++) {
            for (int j = 0; j < a_height; j++) {
                for (int k = 0; k < a_width; k++) {
                    // set the real part
                    ret[i][j][2 * k] = amp[i][j][k] * (float)Math.cos((double)phase[i][j][k]);
                    // set the imaginary part
                    ret[i][j][2 * k + 1] = amp[i][j][k] * (float)Math.sin((double)phase[i][j][k]);
                }
            }
        }

        return ret;
    }
    
    static public float[][][][] toFFTform(float[][][][] amp, float[][][][] phase) {
        int a_frames = amp.length;
        int a_slices = amp[0].length;
        int a_height = amp[0][0].length;
        int a_width = amp[0][0][0].length;
        float[][][][] ret = new float[a_frames][a_slices][a_height][2 * a_width];

        for (int i = 0; i < a_frames; i++) {
            ret[i] = toFFTform(amp[i], phase[i]);
        }
        
        return ret;
    }
    
    // given a real and imaginary matrix, build a complex matrix compatible with the FFT package
    static public float[][][] toFFTformRect(float[][][] reMat, float[][][] imMat) {
        int a_slices = reMat.length;
        int a_height = reMat[0].length;
        int a_width = reMat[0][0].length;
        float[][][] ret = new float[a_slices][a_height][2 * a_width];
        // loop over elements of amp and phase, add appropriate values to ret
        for (int i = 0; i < a_slices; i++) {
            for (int j = 0; j < a_height; j++) {
                for (int k = 0; k < a_width; k++) {
                    // set the real part
                    ret[i][j][2 * k] = reMat[i][j][k];
                    // set the imaginary part
                    ret[i][j][2 * k + 1] = imMat[i][j][k];
                }
            }
        }

        return ret;
    }
    
    static public float[][][][] toFFTformRect(float[][][][] reMat, float[][][][] imMat) {
        int a_frames = reMat.length;
        int a_slices = reMat[0].length;
        int a_height = reMat[0][0].length;
        int a_width = reMat[0][0][0].length;
        float[][][][] ret = new float[a_frames][a_slices][a_height][2 * a_width];

        for (int i = 0; i < a_frames; i++) {
            ret[i] = toFFTformRect(reMat[i], imMat[i]);
        }
        
        return ret;
    }
    
    // takes a complex matrix and squish it back to a simple amplitude matrix
    static public float[][][] getAmplitudeMat(float[][][] mat) {
        int slices = mat.length;
        int height = mat[0].length;
        // the complex matrix has twice the width as the output matrix
        int width = (int)(mat[0][0].length / 2);
        float[][][] ret = new float[slices][height][width];
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    ret[i][j][k] = (float)Math.sqrt((double)mat[i][j][2*k] * (double)mat[i][j][2*k] + (double)mat[i][j][2*k + 1] * (double)mat[i][j][2*k + 1]); 
        
        return ret;
    }
    
    static public float[][][][] getAmplitudeMat(float[][][][] mat) {
        int a_frames = mat.length;
        int a_slices = mat[0].length;
        int a_height = mat[0][0].length;
        int a_width = mat[0][0][0].length / 2;
        float[][][][] ret = new float[a_frames][a_slices][a_height][a_width];
        
        for (int i = 0; i < a_frames; i++)
            ret[i] = getAmplitudeMat(mat[i]);
        
        return ret;
    }
    
    // takes a complex matrix and returns a phase matrix
    static public float[][][] getPhaseMat(float[][][] mat) {
        int slices = mat.length;
        int height = mat[0].length;
        // the complex matrix has twice the width as the output matrix
        int width = (int)(mat[0][0].length / 2);
        float[][][] ret = new float[slices][height][width];
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    ret[i][j][k] = (float)Math.atan2(mat[i][j][2*k + 1], mat[i][j][2*k]); 
        
        return ret;
    }
    
    static public float[][][][] getPhaseMat(float[][][][] mat) {
        int a_frames = mat.length;
        int a_slices = mat[0].length;
        int a_height = mat[0][0].length;
        int a_width = mat[0][0][0].length / 2;
        float[][][][] ret = new float[a_frames][a_slices][a_height][a_width];
        
        for (int i = 0; i < a_frames; i++)
            ret[i] = getPhaseMat(mat[i]);
        
        return ret;
    }
    
    // takes a complex matrix and returns just the real parts
    static public float[][][] getReMat(float[][][] mat) {
        int slices = mat.length;
        int height = mat[0].length;
        // the complex matrix has twice the width as the output matrix
        int width = (int)(mat[0][0].length / 2);
        float[][][] ret = new float[slices][height][width];
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    ret[i][j][k] = mat[i][j][2*k]; 
            
        return ret;
    }
    
    static public float[][][][] getReMat(float[][][][] mat) {
        int a_frames = mat.length;
        int a_slices = mat[0].length;
        int a_height = mat[0][0].length;
        int a_width = mat[0][0][0].length / 2;
        float[][][][] ret = new float[a_frames][a_slices][a_height][a_width];
        
        for (int i = 0; i < a_frames; i++)
            ret[i] = getReMat(mat[i]);
        
        return ret;
    }
    
    // takes a complex matrix and returns just the imaginary parts
    static public float[][][] getImMat(float[][][] mat) {
        int slices = mat.length;
        int height = mat[0].length;
        // the complex matrix has twice the width as the output matrix
        int width = (int)(mat[0][0].length / 2);
        float[][][] ret = new float[slices][height][width];
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    ret[i][j][k] = mat[i][j][2*k + 1]; 
            
        return ret;
    }
    
    static public float[][][][] getImMat(float[][][][] mat) {
        int a_frames = mat.length;
        int a_slices = mat[0].length;
        int a_height = mat[0][0].length;
        int a_width = mat[0][0][0].length / 2;
        float[][][][] ret = new float[a_frames][a_slices][a_height][a_width];
        
        for (int i = 0; i < a_frames; i++)
            ret[i] = getImMat(mat[i]);
        
        return ret;
    }
    
    static public float[][][] increment(float[][][] mat, float inc) {
        int slices = mat.length;
        int height = mat[0].length;
        int width = mat[0][0].length;
        float[][][] ret = new float[slices][height][width];
        
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    ret[i][j][k] = mat[i][j][k] + inc;
        
        return ret;
    }
    
    static public float[][][][] increment(float[][][][] mat, float inc) {
        int frames = mat.length;
        float[][][][] ret = new float[frames][mat[0].length][mat[0][0].length][mat[0][0][0].length];
        
        for (int i = 0; i < frames; i++)
            ret[i] = increment(mat[i], inc);
        
        return ret;
    }
    
    // adds inc to each real element of a complex 3D matrix
    static public float[][][] incrementComplex(float[][][] mat, float inc) {
        int slices = mat.length;
        int height = mat[0].length;
        int width = (int)(mat[0][0].length / 2);
        float[][][] ret = new float[slices][height][2*width];
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++) {
                    ret[i][j][2*k] = mat[i][j][2*k] + inc; 
                    ret[i][j][2*k + 1] = mat[i][j][2*k + 1];
                }
        
        return ret;
    }
    
    // divides, multiplies, subtracts, or adds corresponding elements in two 3D matrices
    // these are complex matrices, so additional operations are required
    static public void matrixOperations(float[][][] mat1, float[][][] mat2, float[][][] retMat, String operation) {
        int slices = mat1.length;
        int height = mat1[0].length;
        int width = (int)(mat1[0][0].length / 2);
        
        float c; // Re of mat1 element
        float d; // Im of mat1 element
        float e; // Re of mat2 element
        float f; // Im of mat2 element
        float[] result; // {Re, Im}
        
        if (operation == "divide") {
            for (int i = 0; i < slices; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        c = mat1[i][j][2*k];
                        d = mat1[i][j][2*k+1];
                        e = mat2[i][j][2*k];
                        f = mat2[i][j][2*k + 1];
                        result = complexDivide(c, d, e, f);
                        // real part of dividing two complex numbers
                        retMat[i][j][2*k] = result[0];
                        // imaginary part of dividing two complex numbers
                        retMat[i][j][2*k + 1] = result[1];
                    }
                }
            }
        }
        else if (operation == "multiply") {
            for (int i = 0; i < slices; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        c = mat1[i][j][2*k];
                        d = mat1[i][j][2*k+1];
                        e = mat2[i][j][2*k];
                        f = mat2[i][j][2*k + 1];
                        result = complexMult(c, d, e, f);
                        // real part of multiplying two complex numbers
                        retMat[i][j][2*k] = result[0];
                        // imaginary part of multiplying two complex numbers
                        retMat[i][j][2*k + 1] = result[1];
                    }
                }
            }
        }
        else if (operation == "subtract") {
            for (int i = 0; i < slices; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        c = mat1[i][j][2*k];
                        d = mat1[i][j][2*k+1];
                        e = mat2[i][j][2*k];
                        f = mat2[i][j][2*k + 1];
                        result = complexSub(c, d, e, f);
                        retMat[i][j][2*k] = result[0];
                        retMat[i][j][2*k + 1] = result[1];
                    }
                }
            }
        }
        else {
            for (int i = 0; i < slices; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        c = mat1[i][j][2*k];
                        d = mat1[i][j][2*k+1];
                        e = mat2[i][j][2*k];
                        f = mat2[i][j][2*k + 1];
                        result = complexAdd(c, d, e, f);
                        retMat[i][j][2*k] = result[0];
                        retMat[i][j][2*k + 1] = result[1];
                    }
                }
            }
        }
    }
    
    // returns the complex conjugate of the input matrix
    static public float[][][] complexConj(float[][][] mat) {
        int slices = mat.length;
        int height = mat[0].length;
        int width = (int)(mat[0][0].length / 2);
        float[][][] retMat = new float[slices][height][2*width];
        
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++) {
                    retMat[i][j][2*k] = mat[i][j][2*k];
                    retMat[i][j][2*k + 1] = (-1)*mat[i][j][2*k + 1];
                }
        
        return retMat;
    }
    
    // scales a  matrix (can be real or complex)
    static public float[][][] scaleMat(float[][][] mat, float scale) {
        int slices = mat.length;
        int height = mat[0].length;
        int width = mat[0][0].length;
        float[][][] retMat = new float[slices][height][width];
        
        for (int i = 0; i < slices; i++)
            for (int j = 0; j < height; j++)
                for (int k = 0; k < width; k++)
                    retMat[i][j][k] = scale * mat[i][j][k];
        
        return retMat;
    }
    
    // scales a  matrix (can be real or complex)
    static public float[][][][] scaleMat(float[][][][] mat, float scale) {
        int frames = mat.length;
        int slices = mat[0].length;
        int height = mat[0][0].length;
        int width = mat[0][0][0].length;
        float[][][][] retMat = new float[frames][slices][height][width];
        
        for (int i = 0; i < frames; i++)
            for (int j = 0; j < slices; j++)
                for (int k = 0; k < height; k++) 
                    for (int l = 0; l < width; l++)
                        retMat[i][j][k][l] = scale * mat[i][j][k][l];
                
        
        return retMat;
    }
    
    // covert 3D matrix to ImagePlus image
    static public ImagePlus reassign(float[][][] testMat, String impType, String title) {
        float [][][][] four_dim = {testMat};
        return reassign(four_dim, impType, title);
    }
    
    // covert 4D matrix to ImagePlus image
    static public ImagePlus reassign(float[][][][] testMat, String impType, String title) {
        int frames = testMat.length;
        int width = testMat[0][0][0].length;
        int height = testMat[0][0].length;
        int slices = testMat[0].length;
    
        ImageStack stack = new ImageStack(width, height);
        ImageProcessor ref = new FloatProcessor(testMat[0][0]);
        double min = Double.POSITIVE_INFINITY;
        double max = -Double.POSITIVE_INFINITY;
        for (int i = 1; i <= frames; i++) 
            for (int j = 1; j <= slices; j++) {
                ImageProcessor ip = new FloatProcessor(testMat[i-1][j-1]);
                ip = ip.rotateRight();
                ip.flipHorizontal();
                // convert image stack to correct architecture with scaling
                if (impType != "GRAY32")
                    if (impType == "GRAY16")
                        ip = ip.convertToShortProcessor(true);
                    else
                        ip = ip.convertToByte(true);
                
                double this_min = ip.getMin();
                double this_max = ip.getMax();
                if (this_min < min) min = this_min;
                if (this_max > max) max = this_max;
                stack.addSlice(ip);
            }
        // The hackiest of hacks
        stack.update(new FloatProcessor(new float[][]{{(float)min, (float)max}}));
        ImagePlus result = new ImagePlus(title, stack);
        if (frames > 1) {
            return HyperStackConverter.toHyperStack(result, 1, slices, frames);
        }

        return result;
    }
    
    // shift a 3D matrix so that all values fall between newMin and newMax
    static public void linearShift (float[][][] mat, float newMin, float newMax) {
        float min = mat[0][0][0];
        float max = mat[0][0][0];
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++) 
                for (int k = 0; k < mat[0][0].length; k++) {
                    if (mat[i][j][k] > max)
                        max = mat[i][j][k];
                    if (mat[i][j][k] < min)
                        min = mat[i][j][k];
                }
        
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++) 
                for (int k = 0; k < mat[0][0].length; k++)
                    mat[i][j][k] = (mat[i][j][k] - min)*(newMax - newMin)/(max - min) + newMin;
    }
    
    // invert an entire image
    static public void invert(ImagePlus imp) {
        ImageProcessor ip;
        for (int i = 1; i <= imp.getStackSize(); i++) {
            ip = imp.getStack().getProcessor(i);
            ip.invert();
        }
      }
    
    // normalize a real matrix so that all pixels add to 1
    static public void normalize(float[][][] mat) {
        float total = 0;
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++)
                for (int k = 0; k < mat[0][0].length; k++)
                    total += mat[i][j][k];
        
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++)
                for (int k = 0; k < mat[0][0].length; k++)
                    mat[i][j][k] = mat[i][j][k] / total;
    }
    
    // normalize a real matrix and imaginary matrix so that all amplitudes add to 1
    static public void normalize(float[][][] matRe, float[][][] matIm) {
        float total = 0;
        for (int i = 0; i < matRe.length; i++)
            for (int j = 0; j < matRe[0].length; j++)
                for (int k = 0; k < matRe[0][0].length; k++)
                    total += Math.sqrt(matRe[i][j][k]*matRe[i][j][k] + matIm[i][j][k]*matIm[i][j][k]);
        
        for (int i = 0; i < matRe.length; i++)
            for (int j = 0; j < matRe[0].length; j++)
                for (int k = 0; k < matRe[0][0].length; k++) {
                    matRe[i][j][k] = matRe[i][j][k] / total;
                    matIm[i][j][k] = matIm[i][j][k] / total;
                }
    }
    
    // after taking the inverse Fourier transform, the quadrants of the image are flipped around for some reason. This puts it back to normal.
    static public float[][][] formatIFFT(float[][][] ampMat) {
        int slices = ampMat.length;
        int height = ampMat[0].length;
        int width = ampMat[0][0].length;
        float placehold;
        int halfSlices = (int)(slices / 2);
        int halfHeight = (int)(height / 2);
        int halfWidth = (int)(width / 2);
        float[][][] reformat = new float[slices][height][width];
        for (int i = 0; i < slices; i++) {
            if (i + halfSlices < slices)
                reformat[i + halfSlices] = ampMat[i];
            else
                if (slices % 2 == 0)
                    reformat[i - halfSlices] = ampMat[i];
                else
                    reformat[i - halfSlices - 1] = ampMat[i];
            }
        
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < halfHeight; j++)
                for (int k = 0; k < halfWidth; k++) {
                    placehold = reformat[i][j][k];
                    reformat[i][j][k] = reformat[i][j+halfHeight][k+halfWidth];
                    reformat[i][j+halfHeight][k+halfWidth] = placehold;
                }
            
            for (int j = 0; j < halfHeight; j++)
                for (int k = halfWidth; k < width; k++) {
                    placehold = reformat[i][j][k];
                    reformat[i][j][k] = reformat[i][j+halfHeight][k-halfWidth];
                    reformat[i][j+halfHeight][k-halfWidth] = placehold;
                }
        }
 
        return reformat;
    }
    
    static public float[][][][] formatIFFT(float[][][][] ampMat) {
        int frames = ampMat.length;
        float[][][][] ret = new float[frames][ampMat[0].length][ampMat[0][0].length][ampMat[0][0][0].length];
        for (int i = 0; i < frames; i++)
            ret[i] = formatIFFT(ampMat[i]);
        
        return ret;
    }
    
    // convolve two matrices by elementwise multiplication in Fourier space. mat1, mat2, and ret are all in FFT form
    static public float[][][] fourierConvolve(float[][][] mat1, float[][][] mat2) {
        FloatFFT_3D fft = new FloatFFT_3D((long)mat1.length, (long)mat1[0].length, (long)mat1[0][0].length/2);
        float[][][] mat1FT = new float[mat1.length][mat1[0].length][mat1[0][0].length];
        float[][][] mat2FT = new float[mat1.length][mat1[0].length][mat1[0][0].length];
        float[][][] retMat = new float[mat1.length][mat1[0].length][mat1[0][0].length];
        
        // make copy of mat1 and mat2 so we don't change them
        for (int i = 0; i < mat1.length; i++)
            for (int j = 0; j < mat1[0].length; j++) 
                for (int k = 0; k < mat1[0][0].length; k++) {
                    mat1FT[i][j][k] = mat1[i][j][k];
                    mat2FT[i][j][k] = mat2[i][j][k];
                }       
    
        fft.complexForward(mat1FT);
        fft.complexForward(mat2FT);
        
        matrixOperations(mat1FT, mat2FT, retMat, "multiply");
        fft.complexInverse(retMat, true);
        
        float[][][] reMat = getReMat(retMat);
        float[][][] imMat = getImMat(retMat);
        reMat = formatIFFT(reMat);
        imMat = formatIFFT(imMat);
        
        retMat = toFFTformRect(reMat, imMat);
        
        return retMat;
    }
    
    // normalize a convolved image so it has the same minimum and maximum amplitude as the original image
    static public void fitConvolution(float[][][] convolved, float[][][] original) {
        int slices = convolved.length;
        int height = convolved[0].length;
        int width = convolved[0][0].length / 2;
        float[][][] originalAmps = getAmplitudeMat(original);
        float[][][] convolvedAmpsOld = getAmplitudeMat(convolved);
        float[][][] convolvedAmpsNew = getAmplitudeMat(convolved);
    
        float min = minOf(originalAmps);
        float max = maxOf(originalAmps);
            
        linearShift(convolvedAmpsNew, min, max);
        for (int j = 0; j < slices; j++)
            for (int k = 0; k < height; k++)
                for (int l = 0; l < width; l++) {
                    convolved[j][k][2*l] = convolved[j][k][2*l] * convolvedAmpsNew[j][k][l] / convolvedAmpsOld[j][k][l];
                    convolved[j][k][2*l + 1] = convolved[j][k][2*l + 1] * convolvedAmpsNew[j][k][l] / convolvedAmpsOld[j][k][l];
                }               
    }
    
    // find error of a deconvolved image
    static public double getError(float[][][][] guess, float[][][][] image, float[][][] psfMat) {
        int frames = image.length;
        int slices = image[0].length;
        int height = image[0][0].length;
        int width = image[0][0][0].length / 2;
        float[][][][] blurredMat = new float[frames][slices][height][2*width];
        
        // find blurred guess by convolving with PSF
        for (int i = 0; i < frames; i++) {
            blurredMat[i] = fourierConvolve(guess[i], psfMat);
            fitConvolution(blurredMat[i], image[i]);
        }
        
        // find mean percent error of all the pixels
        float originalTotal = 0;
        float difference = 0;
        for (int i = 0; i < frames; i++)
            for (int j = 0; j < slices; j ++)
                for (int k = 0; k < height; k++)
                    for (int l = 0; l < width; l++) {
                        originalTotal += Math.abs(image[i][j][k][l]);
                        difference += Math.abs(Math.abs(blurredMat[i][j][k][l]) - Math.abs(image[i][j][k][l]));
                    }
        
        return difference / originalTotal;
    }
    
    // find minimum of real matrix
    static public float minOf(float[][][] mat) {
        float ret = mat[0][0][0];
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++)
                for (int k = 0; k < mat[0][0].length; k++) {
                    if (ret > mat[i][j][k])
                        ret = mat[i][j][k];
                }
        
        return ret;
    }
    
    // find maximum of real matrix
    static public float maxOf(float[][][] mat) {
        float ret = mat[0][0][0];
        for (int i = 0; i < mat.length; i++)
            for (int j = 0; j < mat[0].length; j++)
                for (int k = 0; k < mat[0][0].length; k++) {
                    if (ret < mat[i][j][k])
                        ret = mat[i][j][k];
                }
        
        return ret;
    }
    
    // generate ID and title list of open images
    static public String[] imageList() {
        String[] titles;
        String[] formatted;
        
        titles = WindowManager.getImageTitles();
        formatted = new String[titles.length + 1];
        
        for (int i = 0; i < titles.length; i++)
            formatted[i] = Integer.toString(i + 1) + ": " + titles[i];
        
        formatted[titles.length] = "<none>";
        
        return formatted;
    }
    
    // gets ID from strings in the style generated above
    static public String getImageTitle(String selection) {
        String after_colon = selection.split(": ")[1];
        
        return after_colon;
    }
    
    static public void resliceER (float[][][][] imageMat) {
        int frames = imageMat.length;
        int slices = imageMat[0].length;
        int height = imageMat[0][0].length;
        int width = imageMat[0][0][0].length;
        float[][] temp = new float[height][width];
        
        for (int i = 0; i < frames; i ++)
            for (int j = 0; j < (slices - 1) / 2; j++) {
                temp = imageMat[i][slices - 1];
                for (int k = slices - 1; k >= 1; k--)
                    imageMat[i][k] = imageMat[i][k-1];
                imageMat[i][0] = temp;
            }
    }
}
