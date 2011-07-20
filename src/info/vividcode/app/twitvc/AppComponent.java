package info.vividcode.app.twitvc;

public interface AppComponent {
	
	/**
	 *初期化処理として最初に一度だけ呼び出される.
	 * @param utils TODO
	 * @param register TODO
	 */
	public void initialize( ComponentUtils utils, ComponentRegister register );
	
	/**
	 *終了処理として最後に一度だけ呼び出される.
	 *ユーザーへの確認が必要な場合など, 必要に応じて処理を中で止めてもよいが, 
	 *ユーザーからの応答を受け取った後など, 必ず処理を戻すこと. 
	 */
	public void dispose();
	
}
