package info.vividcode.twitter;

import info.vividcode.app.twitvc.Config;
import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.util.oauth.OAuthRequestHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class StatusUpdater {
	
	public static Credential mAccessCredentials;
	
	public static void setAccessCredentials( Credential c ) {
		mAccessCredentials = c;
	}
	
	public static String replaceXmlEntityReference( String src ) {
		StringBuilder sb = new StringBuilder();
		for( char c : src.toCharArray() ) {
			if( c == '<' ) {
				sb.append( "&lt;" );
			} else if( c == '>' ) {
				sb.append( "&gt;" );
			} else if( c == '&' ) {
				sb.append( "&amp;" );
			} else {
				sb.append( c );
			}
		}
		return sb.toString();
	}
	
	public static void update( String text, Credential c, String idInReplyTo ) throws Exception {
		
		String[][] bparams = new String[( idInReplyTo != null ? 2 : 1 )][];
		bparams[0] = new String[]{ "status", replaceXmlEntityReference( text ) };
		if( idInReplyTo != null ) {
			bparams[1] = new String[]{ "in_reply_to_status_id", idInReplyTo };
		}
		sendRequestToTwitterApiServer( 
				"http://api.twitter.com/1/statuses/update.json", 
				"POST", null, bparams );
		
	}
	
	public static void favorite( String idStr ) throws Exception {
		
		if( idStr == null ) {
			throw new NullPointerException( "引数 idStr が null です..." );
		}
		
		sendRequestToTwitterApiServer( 
				"http://api.twitter.com/1/favorites/create/" + idStr + ".json", 
				"POST", null, null );
		
	}
	
	public static void retweet( String idStr ) throws Exception {
		
		if( idStr == null ) {
			throw new NullPointerException( "引数 idStr が null です..." );
		}
		sendRequestToTwitterApiServer( 
				"http://api.twitter.com/1/statuses/retweet/" + idStr + ".json", 
				"POST", null, null );
		
	}
	
	public static String sendRequestToTwitterApiServer( String urlStr, String method, String[][] queryParams, String[][] bodyParams ) throws Exception {
		
		// TODO 引数チェック
		
		// OAuth 準備
		Credential c = mAccessCredentials;
		String accToken  = c.getToken();
		String accSecret = c.getSecret();
		String conKey    = Config.getConsumerKey();
		String conSecret = Config.getConsumerSecret();
		
		// cf. http://d.hatena.ne.jp/tototoshi/20100117/1263711661 (OAuth)
		Exception exception = null;
		String responseBody = null;
		// TODO
		//HttpURLConnection conn = null;
		HttpClient httpclient = null;
		InputStream is = null;
		try {
			String secretsStr = conSecret + "&" + accSecret;
			
			OAuthRequestHelper.ParamList aParamList = new OAuthRequestHelper.ParamList( 
					new String[][]{
							{ "oauth_consumer_key", conKey },
							{ "oauth_token", accToken },
							{ "oauth_nonce", OAuthRequestHelper.getNonceString() },
							{ "oauth_signature_method", "HMAC-SHA1" },
							{ "oauth_timestamp", Long.toString( new Date().getTime() / 1000 ) },
							{ "oauth_version", "1.0" },
						} );
			OAuthRequestHelper.ParamList qParamList = null;
			OAuthRequestHelper.ParamList bParamList = null;
			if( queryParams != null ) {
				qParamList = new OAuthRequestHelper.ParamList( queryParams );
			}
			if( bodyParams != null ) {
				bParamList = new OAuthRequestHelper.ParamList( bodyParams );
			}
			OAuthRequestHelper helper = new OAuthRequestHelper( 
					urlStr, method, secretsStr, aParamList, qParamList, bParamList );
			
			// TODO
			// タイムアウトの指定
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout( httpParams, 100000 );
			HttpConnectionParams.setSoTimeout( httpParams, 10000 );
			
			httpclient = new DefaultHttpClient( httpParams );
			HttpUriRequest req = null;
			if( method.equals( "POST" ) ) {
				req = new HttpPost( helper.getUrlStringIncludeQueryParams() );
				((HttpPost) req).setEntity( 
						new StringEntity( helper.getRequestBodyString(), "US-ASCII" ) );
			} else if( method.equals( "GET" ) ) {
				req = new HttpGet( helper.getUrlStringIncludeQueryParams() );
			} else {
				throw new RuntimeException( "サポートされていないリクエストメソッド (" + method + ")" );
			}
			req.addHeader( "Authorization", helper.getAuthorizationHeaderString( "" ) );
			req.addHeader( "Content-Type", "application/x-www-form-urlencoded" );
			HttpResponse res = httpclient.execute( req );
			int statusCode = res.getStatusLine().getStatusCode();
			if( statusCode != 200 ) {
				// 400 番台のエラーの場合はエラーメッセージを取得する
				//String responseBody = null;
				if( true ) {//if( 400 <= statusCode && statusCode < 500 ) {
					BufferedReader br = null;
					StringBuilder  sb = new StringBuilder();
					try {
						br = new BufferedReader( 
								new InputStreamReader( res.getEntity().getContent() ) );
						String s = null;
						while( ( s = br.readLine() ) != null ) {
							sb.append( s );
						}
					} finally {
						br.close();
					}
					responseBody = sb.toString();
				}
				throw new RuntimeException( "Twitter API サーバーからエラーが返ってきました\n" + 
						"レスポンスコード : " + statusCode + " " + 
						res.getStatusLine().getReasonPhrase() +
						( responseBody == null ? "" : "\nレスポンスボディ : " + responseBody ) );
			} else {
				Reader br = null;
				StringBuilder sb = new StringBuilder();
				try {
					char[] buf = new char[4096];
					br = new InputStreamReader( res.getEntity().getContent() );
					while( true ) {
						int len = br.read( buf );
						if( len < 0 ) break;
						sb.append( buf, 0, len );
					}
				} finally {
					br.close();
				}
				responseBody = sb.toString();
			}
			HttpEntity ent = res.getEntity();
			is = ent.getContent();
			System.out.println( is.getClass() );
			
		} catch( Exception ex ) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			throw new Exception( ex );
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
		}
		return responseBody;
	}
	
}
