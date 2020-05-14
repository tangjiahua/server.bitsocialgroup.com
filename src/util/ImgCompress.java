package util;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class ImgCompress {
    private Image img;
    private int width;
    private int height;

    /**
     * 构造函数
     */
    public ImgCompress(String fileName) throws IOException {
        File file = new File(fileName);// 读入文件
        img = ImageIO.read(file);      // 构造Image对象
        width = img.getWidth(null);    // 得到源图宽
        height = img.getHeight(null);  // 得到源图长
    }
    /**
     * 按照宽度还是高度进行压缩
     * @param w int 最大宽度
     * @param h int 最大高度
     */
    public void resizeFix(int w, int h, String addr) throws IOException {
        if (width / height > w / h) {
            resizeByWidth(w, addr);
        } else {
            resizeByHeight(h, addr);
        }
    }
    /**
     * 以宽度为基准，等比例放缩图片
     * @param w int 新宽度
     */
    public void resizeByWidth(int w, String addr) throws IOException {
        int h = (int) (height * w / width);
        resize(w, h, addr);
    }
    /**
     * 以高度为基准，等比例缩放图片
     * @param h int 新高度
     */
    public void resizeByHeight(int h, String addr) throws IOException {
        int w = (int) (width * h / height);
        resize(w, h, addr);
    }
    /**
     * 强制压缩/放大图片到固定的大小
     * @param w int 新宽度
     * @param h int 新高度
     */
    public void resize(int w, int h, String addr) throws IOException {
        // SCALE_SMOOTH 的缩略算法 生成缩略图片的平滑度的 优先级比速度高 生成的图片质量比较好 但速度慢
        BufferedImage image = new BufferedImage(w, h,BufferedImage.TYPE_INT_RGB );
        image.getGraphics().drawImage(img, 0, 0, w, h, null); // 绘制缩小后的图
        File destFile = new File(addr);

        if (!destFile.getParentFile().exists()&&!destFile.isDirectory()){
            destFile.getParentFile().mkdirs();
        }

        FileOutputStream out = new FileOutputStream(destFile); // 输出到文件流
        // 可以正常实现bmp、png、gif转jpg
//        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
//        encoder.encode(image); // JPEG编码
        ImageIO.write(image, "jpg", out);
        out.close();
    }
}
