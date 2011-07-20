package info.vividcode.app.twitvc;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

class TextPanelIncludingLinks extends TextPanel {
	
	static class LinkPosition {
		private int mStartPos;
		private int mEndPos;
		private String mUrl;
		LinkPosition( int startPos, int endPos, String url ) {
			mStartPos = startPos;
			mEndPos   = endPos;
			mUrl      = url;
		}
		int getStartPos() {
			return mStartPos;
		}
		int getEndPos() {
			return mEndPos;
		}
		String getUrlStr() {
			return mUrl;
		}
	}
	
	private MouseInputAdapter mMouseInputAdapter;
	private List<LinkPosition> mLinkPosList;
	
	public TextPanelIncludingLinks( String text, int maxWidth, List<LinkPosition> posList ) {
		super( text, maxWidth );
		mLinkPosList = posList;
		mMouseInputAdapter = new MouseInputAdapter() {
			@Override
			public void mouseMoved( MouseEvent evt ) {
				evt = SwingUtilities.convertMouseEvent( 
						(Component) evt.getSource(), evt, TextPanelIncludingLinks.this );
				double pos = calcMousePositionOnText( evt );
				Cursor cursor = Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR  );
				if( pos != -1 && mLinkPosList != null ) {
					for( LinkPosition linkPos : mLinkPosList ) {
						if( linkPos.getStartPos() <= pos && pos <= linkPos.getEndPos() - 1 ) {
							cursor = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );
							break;
						}
					}
				}
				TextPanelIncludingLinks.this.setCursor( cursor );
			}
			@Override
			public void mouseClicked( MouseEvent evt ) {
				evt = SwingUtilities.convertMouseEvent( 
						(Component) evt.getSource(), evt, TextPanelIncludingLinks.this );
				double pos = calcMousePositionOnText( evt );
				if( pos != -1 && mLinkPosList != null ) {
					for( LinkPosition linkPos : mLinkPosList ) {
						if( linkPos.mStartPos <= pos && pos <= linkPos.mEndPos - 1 ) {
							//System.out.println( linkPos.mUrl );
							try {
								Desktop.getDesktop().browse( new URI( linkPos.mUrl ) );
							} catch( IOException ex ) {
								// TODO Auto-generated catch block
								ex.printStackTrace();
							} catch( URISyntaxException ex ) {
								// TODO Auto-generated catch block
								ex.printStackTrace();
							}
							break;
						}
					}
				}
			}
			/**
			 * イベント発生位置の文字の文字番号 (最初の文字が 0 ) を返す. 
			 * 文字と文字の間をクリックした場合は, 0.5 加算で返す. 
			 * (1 文字目と 2 文字目の間ならば 1.5)
			 * 文字の位置に無いならば -1 を返す
			 * @param evt
			 * @return
			 */
			private double calcMousePositionOnText( MouseEvent evt ) {
				Point p = evt.getPoint();
				// クリックしたのが何文字目に相当するのかチェック
				boolean charClicked = false; // 文字をクリックしたかどうか
				int charCount = 0;
				int cpCount = 0;
				boolean hamidasi = false;
				for( PositionLine pl : mPosLineList ) {
					int y = pl.y;
					int top = y - mFontMetrics.getAscent();
					int bottom = y + mFontMetrics.getDescent();
					if( top <= p.y && p.y <= bottom ) {
						int prevX = -1;
						for( int x : pl.xList ) {
							int cp = mText.codePointAt( charCount );
							int width = mFontMetrics.charWidth( cp );
							if( x <= p.x && p.x <= x + width ) {
								charClicked = true;
								break;
							} else if( prevX != -1 && prevX <= x && x <= p.x ) {
								charClicked = true;
								hamidasi = true;
								break;
							}
							charCount += Character.charCount( cp );
							cpCount += 1;
						}
					} else if( p.y < top ) {
						break;
					} else {
						for( int x : pl.xList ) {
							int cp = mText.codePointAt( charCount );
							charCount += Character.charCount( cp );
							cpCount += 1;
						}
					}
					if( charClicked ) break;
				}
				// 文字がクリックされていた場合に次の処理を行う
				double res = -1;
				if( charClicked ) {
					//System.out.println( "マウスポインタが乗っているのは " + cpCount + " 文字目です!!" );
					res = cpCount;
					if( hamidasi ) res -= 0.5;
				}
				return res;
			}
		};
	}
	
	public MouseInputAdapter getMouseInputAdapter() {
		return mMouseInputAdapter;
	}
	
}
