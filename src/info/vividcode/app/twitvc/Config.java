package info.vividcode.app.twitvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import info.vividcode.twitter.StatusUpdater;
import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.util.json.JsonBoolean;
import info.vividcode.util.json.JsonObject;
import info.vividcode.util.json.JsonValue;

public class Config {
	
	public static String getConsumerKey() {
		return ClientCredentials.KEY;
	}
	public static String getConsumerSecret() {
		return ClientCredentials.SECRET;
	}
	
	static JsonObject mPrefJsonObject = new JsonObject();
	
	public static Boolean getPreferenceBoolean( String prefName ) {
		JsonValue val = mPrefJsonObject.get( prefName );
		if( val == null ) {
			return null;
		}
		if( val.valueType() != JsonValue.ValueType.BOOLEAN_VALUE ) {
			throw new RuntimeException( "型不一致" );
		}
		return val.booleanValue();
	}
	
	public static void putPreferenceBoolean( String prefName, boolean value ) {
		if( value ) {
			mPrefJsonObject.put( prefName, JsonBoolean.TRUE );
		} else {
			mPrefJsonObject.put( prefName, JsonBoolean.FALSE );
		}
	}
	
	public static void putAccessCredentials( Credential cred ) {
        FileWriter fw = null;
        try {
        	fw = new FileWriter( "credential.txt" );
        	fw.write( cred.getToken() + "&" + cred.getSecret() + "&" + cred.getUserIdStr() + "&" + cred.getScreenName() );
        } catch( FileNotFoundException ex ) {
			ex.printStackTrace();
		} catch( IOException ex ) {
			ex.printStackTrace();
		} finally {
			try {
		        if( fw != null ) fw.close();
			} catch( IOException ex ) {
				ex.printStackTrace();
			}
        }
		StatusUpdater.setAccessCredentials( cred );
	}
	public static Credential getAccessCredentials() {
		BufferedReader br = null;
		String k = null, v = null, i = null, n = null;
		String fileName = "credential.txt";
        try {
        	File f = new File( fileName );
        	if( f.exists() ) {
	        	br = new BufferedReader( new FileReader( f ) );
	        	String s = br.readLine();
	        	if( s != null ) {
	        		String[] ss = s.split( "&" );
	        		if( ss.length != 4 ) {
	        			return null;
	        		}
	        		// TODO 長さチェック
	        		k = ss[0];
	        		v = ss[1];
	        		i = ss[2];
	        		n = ss[3];
	        	}
        	}
        } catch( FileNotFoundException ex ) {
			ex.printStackTrace();
		} catch( IOException ex ) {
			ex.printStackTrace();
		} finally {
			try {
		        if( br != null ) br.close();
			} catch( IOException ex ) {
				ex.printStackTrace();
			}
        }
		Credential c = null;
		if( k != null && v != null ) {
			c = new Credential( k, v, i, n ); 
		}
		return c;
	}

}
