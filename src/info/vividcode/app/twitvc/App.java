package info.vividcode.app.twitvc;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

public class App {
	
	public static void main( String[] args ) {
		
		System.out.println( System.getProperty( "user.dir" ) );
		new MainComponent().start();
		
		//test();
	}
	
	private static JFrame f;
	public static void test() {
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				StatusWindow win = new StatusWindow( null );
				StatusWindow.MainPanel mp = win.getMainPanel();
				JPanel p = new JPanel();
				p.setPreferredSize( new Dimension( 48, 48 ) );
				//mp.setProfileImage( p );
				TextPanel tp = new TextPanel( "0123", 200 );
				tp.setBackground( Color.WHITE );
				mp.setNamePanel( tp );
				mp.setTextPanel( new TextPanel( "abcd", 200 ) );
				//win.setSize( 200, 200 );
				win.setMaxWidth( 400 );
				win.pack();
				win.setVisible( true );
				/*
				f = new JFrame();
				JPanel c = new JPanel();
				f.getContentPane().add( c );
				TextPanel p = new TextPanel( "あああああああああ\nbbbb", 200 );
				p.setBorder( new LineBorder( new Color( 0, 0, 0 ) ) );
				c.add( p );
				f.setSize( 200, 200 );
				f.setVisible( true );
				*/
			}
		} );
	}
	
}
