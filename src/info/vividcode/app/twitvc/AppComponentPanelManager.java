package info.vividcode.app.twitvc;

import javax.swing.JPanel;

public interface AppComponentPanelManager {
	
	/**
	 *この処理に関する JPanel を返す. 
	 *show() メソッドが呼び出される前に必ず呼び出される.
	 *@return この処理に関する JPanel
	 */
	public JPanel getPanel();
	
	/**
	 *この処理に関する JPanel がメインウィンドウに表示された後に呼び出される.
	 *@return 不明
	 */
	public boolean show();
	
	/**
	 *この処理に関する JPanel がメインウィンドウから取り除かれる直前に呼び出される.
	 *ユーザーへの確認が必要な場合など, 必要に応じて処理を中で止めてもよいが, 
	 *ユーザーからの応答を受け取った後など, 必ず処理を戻すこと. 
	 *この処理に関する JPanel をメインウィンドウから取り除いてはいけない場合は, false を返すこと. 
	 *その場合, JPanel を取り除く処理は中止される.
	 *@return 取り除いてよい場合は true, 取り除いてはいけない場合は false
	 */
	public boolean hidden();

}
