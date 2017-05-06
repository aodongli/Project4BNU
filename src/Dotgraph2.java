import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Properties;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

/******************************
Author: Nick
Date: April 28 2017
Email: stamdlee AT outlook DOT com
*******************************/

public class Dotgraph2 {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        DrawSee draw = new DrawSee();
    }
}

class Point {
// This is a point class
    private int x, y, r; // (x, y) is the central point vector and r is radius in pixels.

    public Point (int x, int y, int r) {
        this.x = x;
        this.y = y;
        this.r = r;
    }

    public int getX () {
        return this.x;
    }

    public int getY () {
        return this.y;
    }

    public int getR () {
        return this.r;
    }

    public void setR (int r) {
        this.r = r;
    }
}

class DrawSee extends JFrame {

// 1cm = 38pixel
// All of the areas are calculated by r^2 in pixels, getting rid of constant `PI'.
// The area of background circle is 27225, calculated by 165*165 in pixels.

    private int bgdiameter = 330; // background diameter is a constant 8.7cm, here, in pixels
    private int bgradius = 165; // background radius is a constant 8.7cm, here, in pixels

    private int bgx = 165; // x value of background center
    private int bgy = 165; // y value of background center

    private int min_margin = 5; // minimum boundry distance between two points
    private int max_margin = 10000; // maximum boundry distance between two points. I highly recommend that do not set max_margin and keep its value 10000. Otherwise the points will cluster to a local area
    private int sum_area = 5000; // empirical value. I highly recommend that this value do not exceed 10000
    private int min_radius = 7; // minimum radius a point may have. I highly recommend users compute the minimum radius first before making any changes. `r = sqrt(sum_area/n)'
    private int max_radius = 50; // maximum radius a point may have
    private List<Point> points = new ArrayList<Point>(); // list of points

    private int min_num = 11; // minimum points number
    private int max_num = 99; // maximum points number
    private int epoch = 50; // generation epochs

    private int gen_flag = 1; // 1 for random sigma with `(0, 50)'; 2 for computed sigma through `min_radius' and `max_radius'
    private double mean_area = 0; // average area
    private double sigma = 30; // standard deviation

    private Color bgcolor = new Color(0x808080); // background color is gray
    private Color pcolor = new Color(0x000000); // point color is black

    private String dpath = "./pics/"; // directory of produced paintings

    public DrawSee() {
        read_config();
        for (int j = 1; j <= epoch; j++)
            for (int i = min_num; i <= max_num; i++) {
                create_points(i);
                paintComponents(i, j);
            }
    }

    public void read_config () {
        // read parameters in config file into system
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("config");
        Properties p = new Properties();
        try {
            p.load(in);// 将输入流加载到配置对象,以使配置对象可以读取config.propertis信息  
            bgdiameter = Integer.parseInt(p.getProperty("diameter"));
            bgradius = bgdiameter/2;
            bgx = bgradius;
            bgy = bgradius;
            dpath = p.getProperty("dpath");
            min_margin = Integer.parseInt(p.getProperty("min_margin"));
            max_margin = Integer.parseInt(p.getProperty("max_margin"));
            min_radius = Integer.parseInt(p.getProperty("min_radius"));
            max_radius = Integer.parseInt(p.getProperty("max_radius"));
            sum_area = Integer.parseInt(p.getProperty("sum_area"));
            min_num = Integer.parseInt(p.getProperty("min_num"));
            max_num = Integer.parseInt(p.getProperty("max_num"));
            epoch = Integer.parseInt(p.getProperty("epoch"));
            gen_flag = Integer.parseInt(p.getProperty("gen_flag"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int distance (Point p1, Point p2) {
        // distance between two points in pixel^2
        return (int) Math.sqrt((double)((p1.getX()-p2.getX())*(p1.getX()-p2.getX()) + (p1.getY()-p2.getY())*(p1.getY()-p2.getY())));
    }

    public int distance (Point p) {
        // distance between p and background center
        return (int) Math.sqrt((double)((p.getX()-bgx)*(p.getX()-bgx) + (p.getY()-bgy)*(p.getY()-bgy)));
    }

    public boolean is_in_bgboundry (Point p) {
        // determine if p is in background circle
        int d = distance(p);
        int r = p.getR();
        /* Debug information
        System.out.println(d);
        System.out.println("x, y "+p.getX()+", "+p.getY());
        System.out.println((int) Math.sqrt((double)((p.getX()-bgx)*(p.getX()-bgx) + (p.getY()-bgy)*(p.getY()-bgy))));
        System.out.print(bgradius+" "+d+" "+r+" "+max_margin+" "+min_margin+" ");
        System.out.println(min_margin <= bgradius-d-r && bgradius-d-r <= max_margin);
        */
        if (min_margin <= bgradius-d-r && bgradius-d-r <= max_margin) return true;
        else return false;
    }

    public boolean is_not_overlap (Point p) {
        // determine if p is satisfied with other points according to points relationship
        int d = 0;
        int r = p.getR();
        int r2 = 0;
        for (int i = 0; i < points.size(); i++) {
            d = distance(p, points.get(i));
            r2 = points.get(i).getR();
            if (min_margin > d-r-r2 || d-r-r2 > max_margin) return false;
        }
        return true;
    }

    public boolean is_valid_size (Point p) {
        // determine if the point's size is valid
        if (p.getR() >= min_radius && p.getR() <= max_radius) return true;
        else return false;
    }

    /* deprecated
    public void set_all_radius (int r) {
        // set all points radius to a common value
        for (int i = 0; i < points.size(); i++) points.get(i).setR(r);
    }
    */
    
    public void create_points (int n) {
        // generate n point centers according to Gaussian distribution, then justify if the point is a valid point
        // a more efficient method is to use polar axis, which can make sure the point is in bgboundry and avoid is_in_bgboundry() step
        int timeout = 9999999;

        points.clear();
        Random rand = new Random(System.currentTimeMillis());

        mean_area = (double)sum_area/n; // average area
        if (gen_flag == 1)
            sigma = rand.nextInt(50); // standard deviation
        if (gen_flag == 2)
            sigma = (int) (max_radius*max_radius-min_radius*min_radius)/6; // standard deviation by rough computation
        double a = 0; // sampled area
        int r = 0; // compute the according radius of each point
        
        while (points.size() < n) {
            a = sigma*rand.nextGaussian()+mean_area;
            r = (int) Math.sqrt(a);

            Point p = new Point(rand.nextInt(bgdiameter)+r, rand.nextInt(bgdiameter)+r, r);

            if (is_in_bgboundry(p) && is_not_overlap(p) && is_valid_size(p)) {
                points.add(p);
                timeout = 9999999;
            } else timeout = timeout - 1;

            if (timeout == 0) {
                System.out.println("There is no suitable vacancy for current point. Please modify the parameters.");
                System.out.println("Some hints: turn `sum_area', `min_radius', `min_margin' smaller and turn `max_radius', `max_margin' larger.");
                System.out.println("WARNING: If you have set 'gen_flag=2', turn `max_radius' smaller.");
                return;
            }
        }
    }

    public void paintComponents(int n,int m) {
        // generate one painting including `n' points in `m' epoch.
        String path = dpath+n+"-"+m+".jpg";
        File file = new File(path);

        BufferedImage bi = new BufferedImage(bgdiameter, bgdiameter,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();

        //* 增加下面代码使得背景透明
        bi = g.getDeviceConfiguration().createCompatibleImage(bgdiameter, bgdiameter, Transparency.TRANSLUCENT);
        g.dispose();
        g = bi.createGraphics();
        // 背景透明代码结束 */

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(bgcolor);
        g.fillOval(0, 0, bgdiameter, bgdiameter);//画圆形色块

        g.setColor(pcolor);
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            g.fillOval(p.getX()-p.getR(), p.getY()-p.getR(), p.getR()*2, p.getR()*2);
        }
        
        try {
            ImageIO.write(bi, "png", file);
            System.out.println(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
