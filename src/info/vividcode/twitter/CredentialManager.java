package info.vividcode.twitter;

import info.vividcode.util.oauth.OAuthRequestHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class CredentialManager {
	
	public static class Credential {
		private String mToken;
		private String mSecret;
		private String mScreenName;
		private String mUserIdStr;
		public Credential( String token, String secret ) {
			mToken = token;
			mSecret = secret;
		}
		public Credential( String token, String secret, String userIdStr, String screenName ) {
			mToken = token;
			mSecret = secret;
			mUserIdStr = userIdStr;
			mScreenName = screenName;
		}
		public String getToken() {
			return mToken;
		}
		public String getSecret() {
			return mSecret;
		}
		public String getUserIdStr() {
			return mUserIdStr;
		}
		public String getScreenName() {
			return mScreenName;
		}
	}
	
	/*
	public static interface RequestCallback {
		public void onSuccess( Credential credential );
		public void onFailure( Exception ex );
	}
	*/
	
	public String getAuthorizationUrl( Credential credential ) {
		// TODO パーセントエンコード
		return "https://api.twitter.com/oauth/authorize?oauth_token=" + credential.getToken();
	}
	
	public Credential requestTemporaryCredential() {
		
		// TwitVC
		String consumerKey = "qYFABsKcT4mLbi2GzbwGQ";
		String consumerSecret = "llwn1CXnPYJKKBTa13HlyaUum2uX9rixM9LYZ89Gr1s";
		String reqTokenUrl = "https://api.twitter.com/oauth/request_token";
		String accTokenUrl = "https://api.twitter.com/oauth/access_token";
		String authUrl = "https://api.twitter.com/oauth/authorize";
		
		Credential credential = null;
		Exception exception = null;
		HttpClient httpclient = null;
		InputStream is = null;
		try {
			String urlStr = reqTokenUrl;
			String method = "POST";
			String secretsStr = consumerSecret + "&";
			
			OAuthRequestHelper.ParamList paramList = new OAuthRequestHelper.ParamList( 
					new String[][]{
							{ "oauth_consumer_key", consumerKey },
							{ "oauth_nonce", OAuthRequestHelper.getNonceString() },
							{ "oauth_signature_method", "HMAC-SHA1" },
							{ "oauth_timestamp", Long.toString( new Date().getTime() / 1000 ) },
							{ "oauth_version", "1.0" },
							{ "oauth_callback", "oob" },
						} );
			OAuthRequestHelper helper = new OAuthRequestHelper( urlStr, method, secretsStr, paramList, null, null );
			
			httpclient = new DefaultHttpClient();
			HttpPost req = new HttpPost( helper.getUrlStringIncludeQueryParams() );
			req.addHeader( "Authorization", helper.getAuthorizationHeaderString( "" ) );
			HttpResponse res = httpclient.execute( req );
			System.out.println( res.getStatusLine().getStatusCode() );
			HttpEntity ent = res.getEntity();
			is = ent.getContent();
			BufferedReader br = new BufferedReader( 
					new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
			StringBuilder sb = new StringBuilder();
			String s = null;
			while( ( s = br.readLine() ) != null ) {
				sb.append( s );
			}
			String resBody = sb.toString();
			String[] resParams = resBody.split( "&" );
			String resToken = null;
			String resSecret = null;
			for( String s1 : resParams ) {
				String[] ss = s1.split( "=" );
				// TODO ss の長さ確認
				if( ss[0].equals( "oauth_token" ) ) {
					resToken = ss[1];
				} else if( ss[0].equals( "oauth_token_secret" ) ) {
					resSecret = ss[1];
				}
			}
			if( resToken != null && resToken != null ) {
				credential = new Credential( resToken, resSecret );
			}
			
		} catch( Exception ex ) {
			// TODO Auto-generated catch block
			//err.printStackTrace();
			exception = ex;
		} finally {
			try {
				if( is != null ) is.close();
			} catch( IOException ex ) {
				ex.printStackTrace();
			}
			// TODO
			//if( conn != null ) conn.disconnect();
            if( httpclient != null ) {
            	httpclient.getConnectionManager().shutdown();
            }
		}
		
		// TODO credential を渡す
		return credential;
		
	}
	
	public Credential requestAccessCredential( Credential tempCredential, String verifier ) {
		
		// TwitVC
		String consumerKey = "qYFABsKcT4mLbi2GzbwGQ";
		String consumerSecret = "llwn1CXnPYJKKBTa13HlyaUum2uX9rixM9LYZ89Gr1s";
		//String reqTokenUrl = "https://api.twitter.com/oauth/request_token";
		String accTokenUrl = "https://api.twitter.com/oauth/access_token";
		//String authUrl = "https://api.twitter.com/oauth/authorize";
		
		Credential credential = null;
		Exception  exception  = null;
		HttpClient httpclient = null;
		InputStream is = null;
		try {
			String urlStr = accTokenUrl;
			String method = "POST";
			String secretsStr = consumerSecret + "&" + tempCredential.getSecret();
			
			OAuthRequestHelper.ParamList paramList = new OAuthRequestHelper.ParamList( 
					new String[][]{
							{ "oauth_consumer_key", consumerKey },
							{ "oauth_token", tempCredential.getToken() },
							{ "oauth_verifier", verifier },
							{ "oauth_nonce", OAuthRequestHelper.getNonceString() },
							{ "oauth_signature_method", "HMAC-SHA1" },
							{ "oauth_timestamp", Long.toString( new Date().getTime() / 1000 ) },
							{ "oauth_version", "1.0" },
						} );
			OAuthRequestHelper helper = new OAuthRequestHelper( urlStr, method, secretsStr, paramList, null, null );
			
			httpclient = new DefaultHttpClient();
			HttpPost req = new HttpPost( helper.getUrlStringIncludeQueryParams() );
			req.addHeader( "Authorization", helper.getAuthorizationHeaderString( "" ) );
			HttpResponse res = httpclient.execute( req );
			System.out.println( res.getStatusLine().getStatusCode() );
			HttpEntity ent = res.getEntity();
			is = ent.getContent();
			BufferedReader br = new BufferedReader( 
					new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
			StringBuilder sb = new StringBuilder();
			String s = null;
			while( ( s = br.readLine() ) != null ) {
				sb.append( s );
			}
			String resBody = sb.toString();
			String[] resParams = resBody.split( "&" );
			String resToken = null;
			String resSecret = null;
			String userIdStr = null;
			String screenName = null;
			for( String s1 : resParams ) {
				String[] ss = s1.split( "=" );
				// TODO ss の長さ確認
				if( ss[0].equals( "oauth_token" ) ) {
					resToken = ss[1];
				} else if( ss[0].equals( "oauth_token_secret" ) ) {
					resSecret = ss[1];
				} else if( ss[0].equals( "user_id" ) ) {
					userIdStr = ss[1];
				} else if( ss[0].equals( "screen_name" ) ) {
					screenName = ss[1];
				}
			}
			if( resToken != null && resToken != null ) {
				credential = new Credential( resToken, resSecret, userIdStr, screenName );
			}
			
		} catch( Exception ex ) {
			// TODO Auto-generated catch block
			//err.printStackTrace();
			exception = ex;
		} finally {
			try {
				if( is != null ) is.close();
			} catch( IOException ex ) {
				ex.printStackTrace();
			}
			// TODO
			//if( conn != null ) conn.disconnect();
            if( httpclient != null ) {
            	httpclient.getConnectionManager().shutdown();
            }
		}
		
		// TODO credential を渡す
		return credential;
		
	}
	
}
