package info.vividcode.app.twitvc;

import info.vividcode.twitter.StatusUpdater;
import info.vividcode.util.json.JsonObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;

public class StatusWindow extends JWindow {
	
	private static final long serialVersionUID = -105008557776514669L;
	
	private JPopupMenu mPopupMenu;
	private boolean mIsDisposed = false;
	private long mDisposedTime  = -1;
	private boolean mAlreadySetDisposedTime = false;
	private BounderThread mBounderThread = null;
	
	private MainPanel mMainPanel;
	private OperationPanel mOperationPanel;
	
	public StatusWindow( JFrame f ) {
		super( f );
		MyMouseListener listener = new MyMouseListener();
		this.addMouseListener( listener );
		this.addMouseMotionListener( listener );
		mPopupMenu = new JPopupMenu();
		// menu - dispose
		JMenuItem menuItem = new JMenuItem( "Dispose this window" );
		DisposeEventListener pal = new DisposeEventListener();
		menuItem.addActionListener( pal );
		mPopupMenu.add( menuItem );
		// menu - show json
		menuItem = new JMenuItem( "Show a JSON structure of this status" );
		menuItem.addActionListener( new ShowJsonEventListener() );
		mPopupMenu.add( menuItem );
		//Add listener to components that can bring up popup menus.
		PopupListener popupListener = new PopupListener();
		this.addMouseListener( popupListener );
		
		mMainPanel = new MainPanel();
		mOperationPanel = new OperationPanel();
		
		JPanel contentPane = new JPanel();
		this.setContentPane( contentPane );
		contentPane.add( mMainPanel );
		LineBorder border = new LineBorder( new Color( 0x33, 0x33, 0x33 ), 2, true );
		contentPane.setBorder( border );
		//this.add( mOperationPanel );
		contentPane.setLayout( new BoxLayout( contentPane, BoxLayout.Y_AXIS ) );
	}
	
	public void dispose() {
		mIsDisposed = true;
		super.dispose();
	}
	
	public MainPanel getMainPanel() {
		return mMainPanel;
	}
	
	private int mMaxWidth = 300;
	public int getMaxWidth() {
		return mMaxWidth;
	}
	public void setMaxWidth( int maxWidth ) {
		mMaxWidth = maxWidth;
	}
	
	/**
	 * ぴょんぴょこするのに使用
	 * @author nobuoka
	 */
	private class BounderThread extends Thread {
		private boolean exit = false;
		@Override
		public void run() {
			Point s = StatusWindow.this.getLocation();
			int x = 0;
			int y = 0;
			int d = 20;
			double k = Math.PI / d;
			try {
				while( true ) {
					for( int j = 0; j < 2; ++ j ) {
						double deg = 0.0;
						for( int i = 0; i < 20; ++ i ) {
							if( exit || mIsDisposed ) return;
							deg += k;
							x = (int)( Math.sin( deg ) * 40 );
							StatusWindow.this.setLocation( s.x + x, s.y + y );
							Thread.sleep( 30 );
						}
						StatusWindow.this.setLocation( s.x, s.y );
					}
					for( int i = 0; i < 10; ++ i ) {
						Thread.sleep( 500 );
						if( exit || mIsDisposed ) return;
					}
				}
			} catch( InterruptedException ex ) {
				ex.printStackTrace();
			}
		}
		public void requestStop() {
			exit = true;
			mBounderThread = null;
		}
	}
	public void setBoundable( boolean b ) {
		// TODO スレッドセーフにする
		if( b && mBounderThread == null ) {
			mBounderThread = new BounderThread();
			mBounderThread.start();
		} else if( ( ! b ) && mBounderThread != null ) {
			mBounderThread.requestStop();
		}
	}
	
	public void setDisposedTime( long time ) {
		// TODO マルチスレッド
		if( ! mAlreadySetDisposedTime ) {
			mAlreadySetDisposedTime = true;
			new Thread() {
				@Override public void run() {
					try {
						while( true ) {
							Thread.sleep( 1000 );
							if( mIsDisposed ) {
								break;
							}
							if( mDisposedTime != -1 ) {
								long cur = new Date().getTime();
								if( mDisposedTime < cur ) {
									StatusWindow.this.dispose();
									break;
								}
							}
						}
					} catch( InterruptedException ex ) {
						// TODO Auto-generated catch block
						ex.printStackTrace();
					}
				} 
			}.start();
		}
		mDisposedTime = time;
	}
	public long getDisposedTime() {
		return mDisposedTime;
	}
	
	private void showJson() {
		if( mJsonStr == null ) {
			throw new NullPointerException();
		}
		JFrame f = new JFrame();
		f.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		JTextArea textArea = new JTextArea();
		JScrollPane textAreaWrapper = new JScrollPane( textArea );
		f.getContentPane().add( textAreaWrapper );
		textArea.setText( mJsonStr );
		f.setSize( new Dimension( 500, 650 ) );
		f.setLocationRelativeTo( null );
		
		//textAreaWrapper.getViewport().setViewPosition( new Point( 0, 0 ) );
		f.setVisible( true );
		
		try {
			Rectangle rect = textArea.modelToView( 0 );
			Rectangle viewrect = textAreaWrapper.getViewport().getViewRect();
			rect.setSize( 10, viewrect.height );
			textArea.scrollRectToVisible(rect);
			textArea.setCaretPosition( 0 );
		} catch ( BadLocationException ex ) {
			ex.printStackTrace();
		}
	}
	
	private String mJsonStr = null;
	public void setJsonStr( String jsonStr) {
		mJsonStr = jsonStr;
	}
	
	private String mStatusIdStr = null;
	public void setStatusIdStr( String statusIdStr ) {
		mStatusIdStr = statusIdStr;
	}
	
	private class MyMouseListener extends MouseAdapter {
		private long mRemainTime = -1;
		private MouseEvent start;
		@Override public void mousePressed( MouseEvent evt ) {
			// TODO 右か左か確認
			start = evt;
			// 触ると無条件で停止
			setBoundable( false );
			// 無条件で最上部へ
			StatusWindow.this.toFront();
			// 触ってる間は消えない
			if( mDisposedTime != -1 ) {
				mRemainTime = mDisposedTime - new Date().getTime();
				mDisposedTime = -1;
			} else {
				mRemainTime = -1;
			}
		}
		@Override public void mouseReleased( MouseEvent evt ) {
			// 消去までの時間を復帰させる ( + 5s する )
			if( mRemainTime != -1 ) {
				mDisposedTime = new Date().getTime() + mRemainTime + 5000;
			}
			// 長距離移動すると消去までの時間を 1 分伸ばす
			int len1 = Math.abs( evt.getXOnScreen() - start.getXOnScreen() );
			int len2 = Math.abs( evt.getYOnScreen() - start.getYOnScreen() ); 
			System.out.println( evt.getX() ); 
			System.out.println( start.getX() );
			if( 20000 < len1 * len1 + len2 * len2 ) {
				System.out.println( "長い移動!!" + (len1 * len1 + len2 * len2) );
				if( mDisposedTime != -1 ) {
					mDisposedTime += 2 * 60 * 1000;
				}
			} else {
				System.out.println( "短い移動 : " + (len1 * len1 + len2 * len2) );
			}
		}
		@Override public void mouseDragged( MouseEvent evt ) {
			// 無条件で最上部へ?
			StatusWindow.this.toFront();
			// TODO 右か左か確認
			if( start != null ) {
				Point eventLocationOnScreen = evt.getLocationOnScreen();
				StatusWindow.this.setLocation(
						eventLocationOnScreen.x - start.getX(),
						eventLocationOnScreen.y - start.getY() );
			}
		}
		@Override
		public void mouseClicked( MouseEvent evt ) {
			System.out.println( "here?" );
			if( mOperationPanelIsVisible ) {
				StatusWindow.this.getContentPane().remove( mOperationPanel );
				StatusWindow.this.pack();
				mOperationPanelIsVisible = false;
			} else {
				StatusWindow.this.getContentPane().add( mOperationPanel );
				StatusWindow.this.pack();
				mOperationPanelIsVisible = true;
			}
		}
	}
	
	private boolean mOperationPanelIsVisible = false;
	
	// http://download.oracle.com/javase/tutorial/uiswing/components/menu.html#popup
	private class PopupListener extends MouseAdapter {
	    @Override public void mousePressed( MouseEvent evt ) {
	        maybeShowPopup( evt );
	    }
	    @Override public void mouseReleased( MouseEvent evt ) {
	        maybeShowPopup( evt );
	    }
	    private void maybeShowPopup( MouseEvent evt ) {
	        if( evt.isPopupTrigger() ) {
	            mPopupMenu.show( evt.getComponent(),
	                       evt.getX(), evt.getY());
	        }
	    }
	}
	
	private class DisposeEventListener implements ActionListener {
		@Override
		public void actionPerformed( ActionEvent evt ) {
			StatusWindow.this.dispose();
		}
	}
	
	private class ShowJsonEventListener implements ActionListener {
		@Override
		public void actionPerformed( ActionEvent evt ) {
			StatusWindow.this.showJson();
		}
	}
	public void compact() {
		Container c = this.getContentPane();
		c.removeAll();
		c.add( mMainPanel.getCompactPanel() );
		this.pack();
	}
	public void returnFromCompact() {
		Container c = this.getContentPane();
		c.removeAll();
		c.add( mMainPanel );
		this.pack();
	}
	public class MainPanel extends JPanel {
		private Image mProfileImage;
		private TextPanel mNamePanel;
		private TextPanel mRetweeterPanel;
		private TextPanel mTextPanel;
		private JPanel mCompactPanel;
		private MainPanel() {
			this.setLayout( null );
			mCompactPanel = new JPanel() {
				@Override
				public void paintComponent( Graphics g ) {
					super.paintComponent( g );
					g.drawImage( mProfileImage, 0, 0, 36, 36, this );
				}
				@Override
				public Dimension getPreferredSize() {
					return new Dimension( 36, 36 );
				}
			};
		}
		public void setProfileImage( Image image ) {
			mProfileImage = image;
		}
		public void setNamePanel( TextPanel panel ) {
			mNamePanel = panel;
			this.add( panel );
		}
		public void setRetweeter( TextPanel panel ) {
			mRetweeterPanel = panel;
		}
		public void setTextPanel( TextPanel panel ) {
			mTextPanel = panel;
			this.add( panel );
		}
		public JPanel getCompactPanel() {
			return mCompactPanel;
		}
		
		private int mProfileImageMargin = 5;
		private int mProfileImageSize = 36;
		@Override
		public void paint( Graphics g ) {
			Dimension size = this.getSize();
			Insets insets = this.getInsets();
			//System.out.println( "actualWidth : " + size.width + ", " + insets );
			//Dimension pipSize = mProfileImagePanel.getPreferredSize();
			//mProfileImagePanel.setSize( pipSize );
			//mProfileImagePanel.setBounds( 0, 0, pipSize.width, pipSize.height );
			//int resWidth = size.width - pipSize.width - insets.left - insets.right;
			int resWidth = size.width - ( mProfileImageMargin + mProfileImageSize ) - insets.left - insets.right;
			mNamePanel.setMaxWidth( resWidth - 20 ); //
			Dimension d2 = mNamePanel.getPreferredSize();
			//System.out.println( ( resWidth - 20 ) + ", " + d2.width + ", " + d2.height );
			//mNamePanel.setBounds( pipSize.width, 0, d2.width, d2.height );
			mNamePanel.setBounds( mProfileImageMargin + mProfileImageSize, 0, d2.width, d2.height );
			mTextPanel.setMaxWidth( resWidth );
			Dimension tpSize = mTextPanel.getPreferredSize();
			//mTextPanel.setSize( mTextPanel.getPreferredSize() );
			//mTextPanel.setBounds( pipSize.width, d2.height + 0, tpSize.width, tpSize.height );
			mTextPanel.setBounds( mProfileImageMargin + mProfileImageSize, d2.height + 0, tpSize.width, tpSize.height );
			super.paint( g );
		}
		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );
			g.drawImage( mProfileImage, mProfileImageMargin, mProfileImageMargin,
					mProfileImageSize, mProfileImageSize, this );
		}
		@Override
		public Dimension getPreferredSize() {
			Insets insets = this.getInsets();
			int maxSize = StatusWindow.this.getMaxWidth();
			Dimension size = new Dimension();
			//Dimension d1 = mProfileImagePanel.getPreferredSize();
			//int resWidth = maxSize - d1.width - insets.left - insets.right;
			int resWidth = maxSize - ( mProfileImageMargin + 
					mProfileImageSize ) - insets.left - insets.right;
			mNamePanel.setMaxWidth( resWidth - 20 );
			Dimension d2 = mNamePanel.getPreferredSize();
			mTextPanel.setMaxWidth( resWidth );
			//System.out.println( ( resWidth - 20 ) + ", " + d2.width + ", " + d2.height );
			Dimension d3 = mTextPanel.getPreferredSize();
			//size.height = Math.max( d1.height, d2.height + d3.height );
			size.height = Math.max( mProfileImageMargin * 2 + mProfileImageSize, d2.height + d3.height );
			//size.width  = d1.width + Math.max( d2.width + 20, d3.width );
			size.width  = mProfileImageMargin + mProfileImageSize + Math.max( d2.width + 20, d3.width );
			size.height += insets.top + insets.bottom;
			size.width  += insets.left + insets.right;
			//System.out.println( "actualWidth : " + size.width + ", " + insets );
			return size;
		}
	}
	
	private class OperationPanel extends JPanel {
		private static final long serialVersionUID = 7103540581563118041L;
		private JPanel mFavPanel;
		private JPanel mRetweetPanel;
		OperationPanel() {
			this.setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
			// Fav 領域
			mFavPanel = new JPanel() {
				private static final long serialVersionUID = 2428705857708924819L;
				@Override
				public Dimension getMaximumSize() {
					return getPreferredSize();
				}
				@Override
				public Dimension getPreferredSize() {
					int h = 0;
					int v = 0;
					for( Component c : this.getComponents() ) {
						Dimension d = c.getPreferredSize();
						h += d.width;
						v = Math.max( v, d.height );
					}
					// border のぶん
					return new Dimension( h + 2, v + 2 );
				}
			};
			mFavPanel.setLayout( new BorderLayout() );
			mFavPanel.add( new JLabel( "Fav" ) );
			mFavPanel.addMouseListener( new MouseAdapter() {
				MouseAdapter self = this;
				@Override
				public void mouseClicked( MouseEvent evt ) {
					final JPanel panel = (JPanel) evt.getSource();
					panel.removeAll();
					panel.add( new TextPanel( "Faving...", 200 ) );
					OperationPanel.this.validate();
					OperationPanel.this.repaint();
					new Thread() {
						@Override public void run() {
							try {
								StatusUpdater.favorite( mStatusIdStr );
								panel.removeAll();
								panel.add( new JLabel( "Faved!!" ) );
								panel.removeMouseListener( self );
							} catch( Exception ex ) {
								// TODO Auto-generated catch block
								panel.removeAll();
								panel.add( new JLabel( "Fav Faild (click to retry)" ) );
								System.err.println( "Fav に失敗＞＜" );
								ex.printStackTrace();
							}
							OperationPanel.this.validate();
							OperationPanel.this.repaint();
						}
					}.start();
				}
			} );
			mFavPanel.setBorder( new LineBorder( Color.BLACK ) );
			this.add( mFavPanel );
			// RT 領域
			mRetweetPanel = new JPanel() {
				private static final long serialVersionUID = 5839948319705581865L;
				@Override
				public Dimension getMaximumSize() {
					return getPreferredSize();
				}
				@Override
				public Dimension getPreferredSize() {
					int h = 0;
					int v = 0;
					for( Component c : this.getComponents() ) {
						Dimension d = c.getPreferredSize();
						h += d.width;
						v = Math.max( v, d.height );
					}
					// border のぶん
					return new Dimension( h + 2, v + 2 );
				}
			};
			mRetweetPanel.setLayout( new BorderLayout() );
			mRetweetPanel.add( new JLabel( "RT" ) );
			mRetweetPanel.addMouseListener( new MouseAdapter() {
				MouseAdapter self = this;
				@Override
				public void mouseClicked( MouseEvent evt ) {
					final JPanel panel = (JPanel) evt.getSource();
					panel.removeAll();
					panel.add( new JLabel( "Retweeting..." ) );
					OperationPanel.this.validate();
					OperationPanel.this.repaint();
					new Thread() {
						@Override public void run() {
							try {
								StatusUpdater.retweet( mStatusIdStr );
								panel.removeAll();
								panel.add( new JLabel( "Retweeted!!" ) );
								panel.removeMouseListener( self );
							} catch( Exception ex ) {
								// TODO Auto-generated catch block
								panel.removeAll();
								panel.add( new JLabel( "RT Faild (click to retry)" ) );
								System.err.println( "Retweet に失敗＞＜" );
								ex.printStackTrace();
							}
							OperationPanel.this.validate();
							OperationPanel.this.repaint();
						}
					}.start();
				}
			} );
			mRetweetPanel.setBorder( new LineBorder( Color.BLACK ) );
			this.add( mRetweetPanel );
			// 最後のりづけ
			this.add( Box.createHorizontalGlue() );
		}
	}
	
}
