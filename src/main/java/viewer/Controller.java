package viewer;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import mandelbrot.Complex;
import mandelbrot.Mandelbrot;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;

/**
 * Controls the color of the pixels of the canvas.
 */
public class Controller implements Initializable {

    /**
     * Dimension of the grid used to supersample each pixel.
     * The number of subpixels for each pixel is the square of <code>SUPERSAMPLING</code>
     */
    private static final int SUPERSAMPLING = 3;

    @FXML
    private Canvas canvas; /* The canvas to draw on */

    //private Camera camera = Camera.camera0;
    private Camera camera = askWhichCamera(); /* The view to display */

    private Mandelbrot mandelbrot = new Mandelbrot(); /* the algorithm */


    /* positions of colors in the histogram */
    private double[] breakpoints = {0., 0.75, 0.85, 0.95, 0.99, 1.0};
    /* colors of the histogram */
    private Color[] colors_set1 =
            {Color.gray(0.2),
                    Color.gray(0.7),
                    Color.rgb(55, 118, 145),
                    Color.rgb(63, 74, 132),
                    Color.rgb(145, 121, 82),
                    Color.rgb(250, 250, 200)
            };

    private Color[] colors_set2 = {
            Color.gray(0.2),
            Color.rgb(0,47,167),
            Color.rgb(182,120,35),
            Color.rgb(255,244,141),
            Color.rgb(128,128,0),
            Color.rgb(231,62,1)
    };

    private Color[] colors_set3 = {
            Color.rgb(223,109,20),
            Color.rgb(223,255,0),
            Color.rgb(0,255,0),
            Color.rgb(255,0,255),
            Color.rgb(255,9,33),
            Color.rgb(108,2,119)

    };


    private Color[] colors_set4 = {
            Color.rgb(140,34,48),
            Color.rgb(177,98,115),
            Color.rgb(87,23,31),
            Color.rgb(150,10,30),
            Color.rgb(255,122,136),
            Color.rgb(177,83,167)

    };

    /* algorithm to generate the distribution of colors */
    private Histogram histogram = new Histogram(breakpoints, askWhichSetOfColors());

    /**
     * Method called when the graphical interface is loaded
     *
     * @param location  location
     * @param resources resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        render();
    }

    /**
     * compute and display the image.
     */
    private void render() {
        List<Pixel> pixels = getPixels();
        renderPixels(pixels);
    }

    /**
     * display each pixel
     *
     * @param pixels the list of all the pixels to display
     */
    private void renderPixels(List<Pixel> pixels) {
        GraphicsContext context = canvas.getGraphicsContext2D();
        for (Pixel pix : pixels) {
            pix.render(context);
        }
    }

    /**
     * Attributes to each subpixel a color
     *
     * @param subPixels the list of all subpixels to display
     */
    private void setSubPixelsColors(List<SubPixel> subPixels) {
        int nonBlackPixelsCount = countNonBlackSubPixels(subPixels);
        if (nonBlackPixelsCount == 0) return;
        Color[] colors = histogram.generate(nonBlackPixelsCount);
        subPixels.sort(SubPixel::compare);
        int pixCount = 0;
        for (SubPixel pix : subPixels) {
            pix.setColor(colors[pixCount]);
            pixCount++;
            if (pixCount >= colors.length) // remaining subpixels stay black (converge).
                break;
        }
    }


    /**
     * Count how many subpixel diverge.
     *
     * @param subPixels the subpixels to display
     * @return the number of diverging subpixels
     */
    private int countNonBlackSubPixels(List<SubPixel> subPixels) {
        return (int)
                subPixels.stream()
                        .filter(pix -> pix.value != Double.POSITIVE_INFINITY)
                        .count();
    }

    /**
     * Generates the list of all the pixels in the canvas
     *
     * @return the list of pixels
     */
    private List<Pixel> getPixels() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        List<SubPixel> subPixels =
                new ArrayList<>(width * height * SUPERSAMPLING * SUPERSAMPLING);
        List<Pixel> pixels =
                new ArrayList<>(width * height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Pixel pix = preparePixel(x, y);
                subPixels.addAll(pix.getSubPixels());
                pixels.add(pix);
            }
        }
        setSubPixelsColors(subPixels);
        return pixels;
    }

    /**
     * Create the pixel with given coordinates
     *
     * @param x horizontal coordinate of the pixel
     * @param y vertical coordinate of the pixel
     * @return the computed pixel with given coordinates
     */
    private Pixel preparePixel(int x, int y) {
        double width = SUPERSAMPLING * canvas.getWidth();
        double height = SUPERSAMPLING * canvas.getHeight();
        List<SubPixel> sampledSubPixels = new ArrayList<>();
        for (int i = 0; i < SUPERSAMPLING; i++) {
            for (int j = 0; j < SUPERSAMPLING; j++) {
                Complex z =
                        camera.toComplex(
                                ((double) (SUPERSAMPLING * x) + i) / width,
                                1 - ((double) (SUPERSAMPLING * y) + j) / height // invert y-axis
                        );
                double divergence = mandelbrot.divergence(z);
                sampledSubPixels.add(new SubPixel(divergence));
            }
        }
        return new Pixel(x, y, sampledSubPixels);
    }


    // Ask which camera use and which zoom
    private Camera askWhichCamera() {
        double centerX, centerY, width, aspectRatioWidth, aspectRatioHeight;
        Scanner scan = new Scanner(System.in);
        System.out.println("Vous allez être invités à paramétrer la caméra que vous souhaitez (Caméra par défaut : centerX = -0.5, centerY = 0., width = 3, aspectRatio=4./3.)");
        System.out.println(" <!>  N'oubliez pas que d'utiliser une virgule \",\" et PAS un point \".\" pour entrer un nombre à décimales <!> ");
        System.out.println("Saisissez la coordonnée X (centerX) du point depuis lequel vous souhaitez voir la vue centrée (par défaut : -0.5) :");
        centerX = scan.nextDouble();
        System.out.println("Saisissez la coordonnée Y (centerY) du point depuis lequel vous souhaitez voir la vue centrée (par défaut : 0.0) :");
        centerY = scan.nextDouble();
        System.out.println("Saisissez la largeur (width) de la vue désirée (par défaut : 3) :");
        width = scan.nextDouble();
        System.out.println("Saisissez le format d'image (aspectRatio) Width/Height de la vue souhaitée :\nWidth (par défaut : 4.0) :");
        aspectRatioWidth = scan.nextDouble();
        System.out.println("Height (par défaut : 3.0) :");
        aspectRatioHeight = scan.nextDouble();

        System.out.println("Camera paramétrée : centerX=" + centerX + " ; centerY=" + centerY + " ; width =" + width + " ; aspectRatio=" + aspectRatioWidth/aspectRatioHeight);

        return new Camera(centerX, centerY, width, aspectRatioWidth/aspectRatioHeight);
    }


    // Ask which set of colors use
    private Color[] askWhichSetOfColors (){
        int setNumber;
        Scanner scan = new Scanner(System.in);
        System.out.println("Vous êtes maintenant invités à choisir le set de couleurs souhaité :\nSet 1 (Classique - Par défault neutre), Set 2 (Alternatif ), Set 3 (Coloré fluo) ou Set 4 (Nuances de roses style princesse) \nSaisissez 1, 2, 3 ou 4 :");
        setNumber=scan.nextInt();
        System.out.println("Merci d'avoir choisi un set de couleur, la fractale sera affichée d'un instant à l'autre (cela peut prendre plusieurs minutes, soyez patients ;D");
        switch(setNumber) {
            case 1 : return colors_set1;
            case 2 : return colors_set2;
            case 3 : return colors_set3;
            case 4 : return colors_set4;
            default: return colors_set1;
        }
    }
}
