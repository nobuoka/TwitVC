package info.vividcode.app.twitvc;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ProfileImageManager {
	
	private Map<String,Image> mProfileImageMap;
	
	public static interface ImageGettingListener {
		public void onGetImage( Image image );
	}
	
	public ProfileImageManager() {
		// TODO マルチスレッド対応
		mProfileImageMap = new HashMap<String,Image>();
	}
	
	public void getImageByUrl( String imageUrl, ImageGettingListener listenr ) {
		new MyThread( imageUrl, listenr ).start();
	}
	
	public Image getImageByUrl( String imageUrl ) {
		return getImageByUrlInternal( imageUrl );
	}
	
	private class MyThread extends Thread {
		private String mImageUrl;
		private ImageGettingListener mListener;
		public MyThread( String imageUrl, ImageGettingListener listener ) {
			mImageUrl = imageUrl;
			mListener = listener;
		}
		@Override
		public void run() {
			Image image = getImageByUrlInternal( mImageUrl );
			mListener.onGetImage( image );
		}
	}
	
	private Image getImageByUrlInternal( String urlStr ) {
		Image image = mProfileImageMap.get( urlStr );
		if( image == null ) {
			// TODO ネット上から取得
			try {
				image = Toolkit.getDefaultToolkit().createImage( new URL( urlStr ) );
				mProfileImageMap.put( urlStr, image );
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return image;
	}
	
}
