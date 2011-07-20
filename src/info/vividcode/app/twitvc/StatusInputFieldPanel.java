package info.vividcode.app.twitvc;

import info.vividcode.twitter.StatusUpdater;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class StatusInputFieldPanel extends JPanel {
	
	private static final long serialVersionUID = -8159638595275397173L;
	
	private JScrollPane mTextAreaWrapper;
	private JTextArea mTextArea;
	private JTextField mTextFieldOfIdInReplyTo;
	private JPanel mTextCountPanel;
	private StatusInputFieldController mController;
	
	private class TweetingInvocationThread extends Thread {
		private String mText;
		private String mIdInReplyTo;
		TweetingInvocationThread( String text, String idInReplyTo ) {
			mText = text;
			mIdInReplyTo = ( idInReplyTo.length() == 0 ? null : idInReplyTo );
		}
		@Override
		public void run() {
			assert ! SwingUtilities.isEventDispatchThread();
			Exception exception = null;
			try {
				mController.tweet( mText, mIdInReplyTo );
			} catch( Exception ex ) {
				System.err.println( "例外発生のため tweet 失敗" );
				ex.printStackTrace();
				exception = ex;
			}
			final Exception tex = exception;
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					Exception ex = tex;
					if( ex == null ) {
						mTextArea.setText( "" );
						mTextFieldOfIdInReplyTo.setText( "" );
					} else {
						// TODO エラーメッセージ表示
						JOptionPane.showMessageDialog( mTextArea.getParent(), 
								"投稿失敗\n" + "------------\n" + ex.getMessage() );
					}
					mTextArea.setEnabled( true );
					mTextFieldOfIdInReplyTo.setEnabled( true );
					mTextArea.requestFocusInWindow();
				}
			} );
		}
	}
	public StatusInputFieldPanel( StatusInputFieldController cont ) {
		mController = cont;
		this.setLayout( null );
		mTextArea = new JTextArea();
		mTextArea.setLineWrap( true );
		mTextArea.addKeyListener( new KeyAdapter() {
			@Override
			public void keyPressed( KeyEvent evt ) {
				assert SwingUtilities.isEventDispatchThread();
				if( evt.getKeyCode() == KeyEvent.VK_ENTER && 
						( evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK ) != 0 ) {
					String text = mTextArea.getText();
					String idInReplyTo = mTextFieldOfIdInReplyTo.getText();
					// 長さが 0 でなければ投稿を試みる
					if( text.length() != 0 ) {
						mTextArea.setEnabled( false );
						mTextFieldOfIdInReplyTo.setEnabled( false );
						Thread t = new TweetingInvocationThread( text, idInReplyTo );
						t.start();
					}
				}
			}
		} );
		mTextArea.getDocument().addDocumentListener( new DocumentListener() {
			@Override
			public void insertUpdate( DocumentEvent evt ) {
				mTextCountPanel.repaint();
			}
			@Override
			public void removeUpdate( DocumentEvent evt ) {
				mTextCountPanel.repaint();
			}
			@Override
			public void changedUpdate( DocumentEvent evt ) {
				mTextCountPanel.repaint();
			}
		} );
		mTextAreaWrapper = new JScrollPane( mTextArea );
		super.add( mTextAreaWrapper );
		mTextFieldOfIdInReplyTo = new JTextField( 14 );
		super.add( mTextFieldOfIdInReplyTo );
		mTextCountPanel = new JPanel() {
			private static final long serialVersionUID = -4181330386081089410L;
			@Override
			public void paintComponent( Graphics g ) {
				super.paintComponent( g );
				int width  = this.getWidth();
				int height = this.getHeight();
				String s = StatusUpdater.replaceXmlEntityReference( mTextArea.getText() );
				String textCountMessage = Integer.toString( s.codePointCount( 0, s.length() ) );
				FontMetrics fm = g.getFontMetrics();
				int strWidth  = fm.stringWidth( textCountMessage );
				int strHeight = fm.getHeight();
				int vpos = ( height - strHeight ) / 2 + fm.getAscent();
				g.drawString( textCountMessage, width - strWidth - 10, vpos );
			}
		};
		super.add( mTextCountPanel );
		// サロゲートペアを含む文字列もペーストできるように
		mTextArea.getActionMap().put( "paste-from-clipboard", new AbstractAction() {
			private static final long serialVersionUID = -4746377591126279320L;
			@Override
			public void actionPerformed( ActionEvent evt ) {
				Clipboard clp = getToolkit().getSystemClipboard();
				Transferable data = clp.getContents(this);
				
				if( data == null || ! data.isDataFlavorSupported( DataFlavor.stringFlavor ) ) {
					// TODO
					System.out.println( "[DEBUG] クリップボードの中身が文字列ではないのでペーストしません" );
				} else {
					try {
						Object src = evt.getSource();
						if( src instanceof JTextComponent ) {
							JTextComponent tc = (JTextComponent) src;
							int ss = tc.getSelectionStart();
							int se = tc.getSelectionEnd();
							String s1 = tc.getText();
							String s2 = (String) data.getTransferData( DataFlavor.stringFlavor );
							tc.setText( s1.substring( 0, ss ) + s2 +
									s1.substring( se, s1.length() ) );
							tc.setCaretPosition( ss + s2.length() );
						}
					} catch( UnsupportedFlavorException ex ) {
						// TODO Auto-generated catch block
						ex.printStackTrace();
					} catch( IOException ex ) {
						// TODO Auto-generated catch block
						ex.printStackTrace();
					}
				}
			}
		} );
	}
	
	public void setIdInReplyTo( String idStr, String screenName ) {
		mTextFieldOfIdInReplyTo.setText( idStr );
		mTextArea.setText( "@" + screenName + " " + mTextArea.getText() );
		mTextArea.requestFocusInWindow();
	}
	
	@Override
	protected void paintChildren( Graphics g ) {
		Dimension size = this.getSize();
		// 子のコンポーネントのサイズなどを設定
		Dimension sizeC1 = mTextFieldOfIdInReplyTo.getPreferredSize();
		mTextAreaWrapper.setBounds( 0, 0, size.width, size.height - sizeC1.height );
		mTextFieldOfIdInReplyTo.setBounds( 0, size.height - sizeC1.height, sizeC1.width, sizeC1.height );
		mTextCountPanel.setBounds( sizeC1.width, size.height - sizeC1.height, 
				size.width - sizeC1.width, sizeC1.height );
		// 描画
		super.paintChildren( g );
	}
	
}
