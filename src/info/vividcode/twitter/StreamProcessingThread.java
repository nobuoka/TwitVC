package info.vividcode.twitter;

import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.util.oauth.OAuthRequestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class StreamProcessingThread extends Thread {
		
		private Credential mAccessCredential;
		private UserStreamListener mListener;
		
		private class MyJsonStringReaderThread extends Thread {
			
			private final InputStream mIS;
			private final Queue<String> mStringQueue;
			private volatile Throwable mEx;
			
			public MyJsonStringReaderThread( InputStream is ) {
				mIS = is;
				mEx = null;
				mStringQueue = new LinkedList<String>();
			}
			
			@Override
			public void run() {
				try {
					byte[] bytes = new byte[ 4096 ];
					int s = 0; // 開始位置
					int e = 0; // 終了位置
					while( ! Thread.interrupted() ) {
						StringBuilder sb1 = new StringBuilder();
						while( true ) {
							if( s == e ) {
								//System.out.println( "[DEBUG] num of available bytes : " + mIS.available() );
								e = mIS.read( bytes );
								s = 0;
								System.out.println( "[DEBUG] num of readed bytes : " + e );
							}
							int d = 0;
							int hasCR = 0;
							for( d = s; d < e; d++ ) {
								// 0x0D は CR の ASCII コード
								if( bytes[d] == 0x0D ) {
									if( d + 1 < e && bytes[d + 1] == 0x0A ) hasCR++;
									hasCR++;
									break;
								}
							}
							// TODO
							// 文字の切れ目にちょうど bytes の切れ目がこない可能性
							sb1.append( new String( bytes, s, d - s ) );
							s = d;
							if( hasCR != 0 ) {
								s += hasCR;
								break;
							}
						}
						String jsonStr = sb1.toString(); // 中身無し (長さ 0) もありえる
						// 空白だけかどうかのチェック
						boolean isWSS = true;
						for( char c : jsonStr.toCharArray() ) {
							if( c != 0x0A && c != 0x0D ) { isWSS = false; break; }
						}
						if( ! isWSS ) {
							//mListener.onProcess( jsonStr );
							putString( jsonStr );
						}
					}
				} catch( Throwable ex ) {
					//ex.printStackTrace();
					mEx = ex;
				}
			}
			
			private synchronized void putString( String str ) {
				mStringQueue.offer( str );
				this.notifyAll();
			}
			
			public synchronized String readString() throws InterruptedException {
				if( mEx != null ) {
					throw new RuntimeException( "例外発生のため, これ以上の処理は続けられません＞＜", mEx );
				}
				if( ! this.isAlive() ) {
					throw new RuntimeException( "既にスレッドは停止しています" );
				}
				while( mStringQueue.isEmpty() ) {
					System.out.println( "sleep" );
					this.wait();
					System.out.println( "wakeup" );
				}
				return mStringQueue.poll();
			}
			
		}
		
		public StreamProcessingThread( UserStreamListener listener, Credential cred ) {
			super();
			mListener = listener;
			//mRequestedStopping = false;
			if( cred == null ) {
				throw new NullPointerException();
			}
			mAccessCredential = cred;
		}
		
		@Override
		public void run() {
			
			String accToken  = mAccessCredential.getToken();
			String accSecret = mAccessCredential.getSecret();
			String conKey    = "qYFABsKcT4mLbi2GzbwGQ";
			String conSecret = "llwn1CXnPYJKKBTa13HlyaUum2uX9rixM9LYZ89Gr1s";
			
			// cf. http://d.hatena.ne.jp/tototoshi/20100117/1263711661 (OAuth)
			Exception exception = null;
			// TODO
			//HttpURLConnection conn = null;
			HttpClient httpclient = null;
			InputStream is = null;
			try {
				mListener.onStart();
				String urlStr = "https://userstream.twitter.com/2/user.json";
				String method = "GET";
				String secretsStr = conSecret + "&" + accSecret;
				
				OAuthRequestHelper.ParamList paramList = new OAuthRequestHelper.ParamList( 
						new String[][]{
								{ "oauth_consumer_key", conKey },
								{ "oauth_token", accToken },
								{ "oauth_nonce", OAuthRequestHelper.getNonceString() },
								{ "oauth_signature_method", "HMAC-SHA1" },
								{ "oauth_timestamp", Long.toString( new Date().getTime() / 1000 ) },
								{ "oauth_version", "1.0" },
							} );
				OAuthRequestHelper helper = new OAuthRequestHelper( urlStr, method, secretsStr, paramList, null, null );
				
				/*
				URL url = new URL( helper.getUrlStringIncludeQueryParams() );
				URLConnection tmpConn = url.openConnection();
				conn = (HttpURLConnection)tmpConn;
				conn.setRequestProperty( "Authorization", helper.getAuthorizationHeaderString( "" ) );
				
				conn.connect();
				int resCode = conn.getResponseCode();
				if( resCode != 200 ) {
					// TODO
					throw new RuntimeException( "err" );
				}
				is = conn.getInputStream();
				// TODO
				/*
				BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
				String line = null;
				while( true ) {
					//if( br.ready() ) {
						line = br.readLine();
						try {
							//mListener.onProcess( line );
							System.out.println( line );
						} catch( Exception ex ) {
							ex.printStackTrace();
						}
					//}
					if( mRequestedStopping ) break;
				}
				br.close();
				*/
				
				// TODO
				// タイムアウトの指定
				HttpParams httpParams = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout( httpParams, 5000 );
				HttpConnectionParams.setSoTimeout( httpParams, 60000 );
				
				httpclient = new DefaultHttpClient( httpParams );
				HttpGet req = new HttpGet( helper.getUrlStringIncludeQueryParams() );
				req.addHeader( "Authorization", helper.getAuthorizationHeaderString( "" ) );
				HttpResponse res = httpclient.execute( req );
				if( res.getStatusLine().getStatusCode() != 200 ) {
					throw new RuntimeException( "正常に接続できませんでした" );
				}
				HttpEntity ent = res.getEntity();
				is = ent.getContent();
				MyJsonStringReaderThread readerThread = new MyJsonStringReaderThread( is );
				readerThread.start();
				try {
					while( ! Thread.interrupted() ) {
						String jsonStr = readerThread.readString();
						mListener.onProcess( jsonStr );
					}
				} catch( InterruptedException ex ) { /* do nothing */ }
				req.abort();
				is = null; // abort すると is は close される?
				readerThread.interrupt();
				//process( is );//*/
			} catch( Exception ex ) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				exception = ex;
			} finally {
				try {
					// 明示的に閉じる必要はあるのか?
					if( is != null ) is.close();
				} catch( IOException ex ) {
					ex.printStackTrace();
				}
				// TODO
				//if( conn != null ) conn.disconnect();
	            if( httpclient != null ) httpclient.getConnectionManager().shutdown();
				mListener.onStop( exception );
			}
		}
		
		public void requestStop() {
			this.interrupt();
		}
		
}
