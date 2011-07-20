package info.vividcode.app.twitvc;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class TextPanel extends JPanel {
	
	private static final long serialVersionUID = -2635285113798259993L;
	private int mMaxWidth;
	protected String mText;
	protected List<PositionLine> mPosLineList;
	protected FontMetrics mFontMetrics;
	
	protected class PositionLine {
		public List<Integer> xList;
		public int y;
		PositionLine( int y ) {
			this.xList = new ArrayList<Integer>();
			this.y = y;
		}
	}
	
	public TextPanel( String text, int maxWidth ) {
		mText = text;
		mMaxWidth = maxWidth;
		mPosLineList = new ArrayList<PositionLine>();
	}
	
	public void setMaxWidth( int width ) {
		mMaxWidth = width;
	}
	
	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		mPosLineList.clear();
		//g.drawString( mText, 0, 10 );
		int hPadding = 5;
		Insets insets = this.getInsets();
		int maxWidth = this.getSize().width - hPadding * 2 - insets.left - insets.right;
		FontMetrics m = this.getFontMetrics( this.getFont() );
		mFontMetrics = m;
		int topPadding = m.getLeading();
		int leftS = hPadding + insets.left;
		int topS  = topPadding + insets.top + m.getAscent();
		// 1 行で収まるかどうか
		//int width = m.stringWidth( mText );
		//if( width <= maxWidth ) {
		//	g.drawString( mText, leftS, topS );
		//} else {
		{
			// 複数行の場合
			int maxLineWidth = 0;
			int numLines = 1;
			{
				StringBuilder sb = new StringBuilder();
				int i = 0;
				int curLineWidth = 0;
				PositionLine posLine = new PositionLine( topS );
				mPosLineList.add( posLine );
				while( i < mText.length() ) {
					int cp = mText.codePointAt( i );
					i += Character.charCount( cp );
					int charWidth = m.charWidth( cp );
					// 改行コードを考慮
					boolean isLB = false;
					if( cp == 0x0A || cp == 0x0D ) {
						charWidth = 0;
						isLB = true;
					}
					if( ( ! isLB ) && 
							( curLineWidth == 0 || curLineWidth + charWidth <= maxWidth ) ) {
						posLine.xList.add( leftS + curLineWidth );
						curLineWidth += charWidth;
						sb.appendCodePoint( cp );
					} else {
						maxLineWidth = Math.max( maxLineWidth, curLineWidth );
						g.drawString( sb.toString(), leftS, topS + ( numLines - 1 ) * m.getHeight() );
						posLine = new PositionLine( topS + numLines * m.getHeight() );
						posLine.xList.add( leftS );
						mPosLineList.add( posLine );
						curLineWidth = charWidth;
						numLines ++;
						sb = new StringBuilder();
						sb.appendCodePoint( cp );
					}
				}
				maxLineWidth = Math.max( maxLineWidth, curLineWidth );
				g.drawString( sb.toString(), leftS, topS + ( numLines - 1 ) * m.getHeight() );
			}
		}
	}
	
	@Override
	public Dimension preferredSize() {
		int hPadding = 5;
		FontMetrics m = this.getFontMetrics( this.getFont() );
		Insets insets = this.getInsets();
		int maxWidth = mMaxWidth - hPadding * 2 - insets.left - insets.right;
		// 1 行で収まるかどうか
		//int width = m.stringWidth( mText );
		//if( width <= maxWidth ) {
		//	return new Dimension( width + hPadding * 2 + insets.left + insets.right,
		//			m.getHeight() + m.getLeading() + insets.top + insets.bottom );
		//}
		// 複数行の場合
		int maxLineWidth = 0;
		int numLines = 1;
		{
			int i = 0;
			int curLineWidth = 0;
			int len = mText.length();
			while( i < len ) {
				int cp = mText.codePointAt( i );
				i += Character.charCount( cp );
				int charWidth = m.charWidth( cp );
				// 改行コードを考慮
				boolean isLB = false;
				if( cp == 0x0A || cp == 0x0D ) {
					charWidth = 0;
					isLB = true;
				}
				if( ( ! isLB ) && 
						( curLineWidth == 0 || curLineWidth + charWidth <= maxWidth ) ) {
					// 改行しない場合の処理
					curLineWidth += charWidth;
				} else {
					// 改行処理
					maxLineWidth = Math.max( maxLineWidth, curLineWidth );
					curLineWidth = charWidth;
					numLines ++;
				}
			}
			maxLineWidth = Math.max( maxLineWidth, curLineWidth );
		}
		return new Dimension( maxLineWidth + hPadding * 2 + insets.left + insets.right, 
				numLines * m.getHeight() + m.getLeading() + insets.top + insets.bottom );
	}
	
}
