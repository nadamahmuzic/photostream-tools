/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hochschuledarmstadt.photostream_tools;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import hochschuledarmstadt.photostream_tools.model.Photo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ImageCacherTest {

    public static final String ROOT = "";
    private Context context;
    private ImageCacher imageCacher;
    private File imageFile;

    private void educateMock(String fileName) {
        when(context.getFileStreamPath(fileName)).thenReturn(new File(ROOT, fileName));
        try {
            when(context.openFileOutput(fileName, Context.MODE_PRIVATE)).thenReturn(new FileOutputStream(new File(ROOT, fileName)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() {
        context = mock(Context.class);
        when(context.getFilesDir()).thenReturn(new File(ROOT));
        imageCacher = new ImageCacher(context);
    }

    @After
    public void tearDown() {
        if (imageFile != null && imageFile.exists())
            imageFile.delete();
    }

    @Test
    public void isImageCached() {
        assertFalse(imageCacher.isCached(Integer.MAX_VALUE));
    }

    @Test
    public void cacheNewImage() {
        Photo photo = buildPhotoForImageCacher();
        try {
            assertTrue(imageCacher.cacheImage(photo));
            assertTrue(imageCacher.isCached(photo.getId()));
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

    @Test
    public void detectImageAlreadyCached() {
        Photo photo = buildPhotoForImageCacher();
        try {
            assertTrue(imageCacher.cacheImage(photo));
            assertTrue(imageCacher.cacheImage(photo));
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    @Test
    public void cacheImageWithBytes() {
        Photo photo = buildPhotoForImageCacher();
        String base64Bytes = photo.getImageFilePath();
        byte[] data = Base64.decode(base64Bytes, Base64.DEFAULT);
        try {
            assertTrue(imageCacher.cacheImage(photo, data));
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    private Photo buildPhotoForImageCacher() {
        Gson gson = new Gson();
        Photo photo = gson.fromJson(Fakes.PHOTO_RESULT, Photo.class);
        String fileName = String.format("%s.jpg", photo.getId());
        imageFile = new File(ROOT, fileName);
        String imageFilePath = imageFile.getAbsolutePath();
        educateMock(fileName);
        photo = Fakes.buildFakePhoto(photo.getId(), imageFilePath, photo.getDescription(), photo.isLiked(), photo.isDeleteable(), photo.getCommentCount());
        return photo;
    }

}
