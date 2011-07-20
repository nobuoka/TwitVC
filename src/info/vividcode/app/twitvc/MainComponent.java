package info.vividcode.app.twitvc;

import info.vividcode.twitter.StatusUpdater;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

public class MainComponent {
	
	private JFrame mMainFrame;
	private JPanel mMainPanel;
	private AppComponentPanelManager mActiveProcessingComponent;
	
	private List<AppComponent> mComponentList;
	private Map<String,AppComponentPanelManager> mPanelManagingComponentMap;
	private List<String> mPanelManagingComponentNameList;
	/** EventName : EventProcessingComponentList */
	private Map<String,List<AppComponentEventHandler>> mEventHandlingComponentsMap;
	
	private ComponentSelectPanel mComponentSelectPanel;
	
	private ComponentUtils mComponentUtils;
	
	private class MyComponentUtils implements ComponentUtils {
		
		private JFrame mMainFrame;
		
		MyComponentUtils( JFrame mainFrame ) {
			mMainFrame = mainFrame;
		}
		
		@Override
		public void makeMainFrameActive() {
			assert SwingUtilities.isEventDispatchThread();
			mMainFrame.toFront();
		}
		
		public void showMessageDialog( String message ) {
			JOptionPane.showMessageDialog( mMainFrame, message );
		}
		
		public JWindow createJWindow() {
			return new JWindow( mMainFrame );
		}
		public <T extends JWindow> T createJWindow( Class<T> c ) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
			Constructor<T> co = c.getConstructor( JFrame.class );
			return co.newInstance( mMainFrame );
		}
		
		public void raiseEvent( final AppEvent evt ) {
			// TODO スレッド調整
			new Thread() {
				@Override public void run() {
					String name = evt.getEventName();
					System.out.println( "[INFO] AppEvent 発生 : " + name );
					List<AppComponentEventHandler> list = mEventHandlingComponentsMap.get( name );
					if( list != null ) {
						System.out.println( "[INFO] AppEvent の受け手数 : " + list.size() );
						for( AppComponentEventHandler handler : list ) {
							handler.listenEvent( evt );
						}
					} else {
						System.out.println( "[INFO] AppEvent の受け手数 : 0, " + mEventHandlingComponentsMap );
					}
				}
			}.start();
		}
		
	}
	
	private class MainFrameListener extends WindowAdapter {
		@Override
		public void windowOpened( WindowEvent evt ) {
			System.out.println( "Window opend!!" );
		}
		
		@Override
		public void windowClosing( WindowEvent evt ) {
			// TODO 初期化処理中に終了処理に入った場合の整合性とか
			System.out.println( "Window closing!!" );
			new Thread() {
				@Override
				public void run() {
					MainComponent.this.stop();
				}
			}.start();
		}
		
		@Override
		public void windowClosed( WindowEvent evt ) {
			System.out.println( "Window closed!!" );
		}
	}
	
	private class ComponentSelectPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 397167553924043014L;
		private static final String NONE_NAME = "----";
		private String[] mComponentNames;
		private JComboBox mComboBox;
		public ComponentSelectPanel( String[] componentNames ) {
			super();
			//mComponentNames = new String[ componentNames.length + 1 ];
			//mComponentNames[0] = NONE_NAME;
			mComponentNames = new String[ componentNames.length ];
			for( int i = 0; i < componentNames.length; ++i ) {
				mComponentNames[i] = componentNames[i];
			}
			mComboBox = new JComboBox( mComponentNames );
			mComboBox.addActionListener( this );
			this.add( mComboBox );
		}
		@Override
	    public void actionPerformed( ActionEvent evt ) {
	        JComboBox cb = (JComboBox) evt.getSource();
	        final String componentName = (String) cb.getSelectedItem();
	        if( ! componentName.equals( NONE_NAME ) ) {
		        new Thread() {
		        	@Override public void run() {
		    	        MainComponent.this.changeProcessingComponent( componentName );
		        	}
		        }.start();
	        }
	    }
		public void setEnabled( boolean b ) {
			mComboBox.setEnabled( b );
		}
		public void changeSelectedItem( String name ) {
			// イベントディスパッチスレッドから呼ぶこと
			assert SwingUtilities.isEventDispatchThread();
			mComboBox.removeActionListener( this );
			int index = mComponentNames.length - 1;
			while( index > 0 ) {
				if( name.equals( mComponentNames[ index ] ) ) {
					break;
				}
				-- index;
			}
			mComboBox.setSelectedIndex( index );
			mComboBox.addActionListener( this );
		}
	}
	
	MainComponent() {
		// イベントディスパッチスレッドから呼んではいけない
		assert ! SwingUtilities.isEventDispatchThread();
		mActiveProcessingComponent = null;
		//mProcessingComponentMap = new HashMap<String,AppComponentPanelManager>();
		mComponentList = new ArrayList<AppComponent>();
		mPanelManagingComponentMap = new HashMap<String,AppComponentPanelManager>();
		mPanelManagingComponentNameList = new ArrayList<String>();
		mEventHandlingComponentsMap = new HashMap<String,List<AppComponentEventHandler>>();
	}
	
	ComponentUtils getComponentUtils() {
		return mComponentUtils;
	}
	
	void start() {
		StatusUpdater.setAccessCredentials( Config.getAccessCredentials() );
		// イベントディスパッチスレッドから呼んではいけない
		assert ! SwingUtilities.isEventDispatchThread();
		mMainFrame = new JFrame();
		mComponentUtils = new MyComponentUtils( mMainFrame );
		// ウィンドウ表示
		// TODO
		//showInitPanel();
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				mMainFrame.addWindowListener( new MainFrameListener() );
				mMainFrame.setSize( 340, 250 );
				JPanel p = new TextPanel( "初期化中です...", 200 );
				mMainFrame.getContentPane().add( p );
				mMainFrame.setVisible( true );
			}
		} );
		// コンポーネント準備
		final ComponentUtils cu = this.getComponentUtils();
		List<Class<? extends AppComponent>> componentClassList = new ArrayList<Class<? extends AppComponent>>();
		componentClassList.add( UserStreamComponent.class );
		componentClassList.add( ConfigComponent.class );
		componentClassList.add( AuthComponent.class );
		componentClassList.add( StatusDisplayingComponent.class );
		
		ComponentRegister register = new ComponentRegister() {
			@Override
			public void registPanelManager( 
					AppComponentPanelManager manager, String panelName ) {
				// TODO チェックなど
				mPanelManagingComponentNameList.add( panelName );
				mPanelManagingComponentMap.put( panelName, manager );
			}
			@Override
			public void registEventHandler(
					AppComponentEventHandler handler, String eventName) {
				// TODO チェックなど
				List<AppComponentEventHandler> list = 
					mEventHandlingComponentsMap.get( eventName );
				if( list == null ) {
					list = new ArrayList<AppComponentEventHandler>();
					mEventHandlingComponentsMap.put( eventName, list );
				}
				list.add( handler );
			}
		};
		// コンポーネントのインスタンス化, 初期化
		for( Class<? extends AppComponent> c : componentClassList ) {
			try {
				AppComponent comp = c.newInstance();
				mComponentList.add( comp );
				comp.initialize( cu, register );
			} catch( final Exception ex ) {
				// TODO Auto-generated catch block
				// コンポーネントの初期化に失敗
				ex.printStackTrace();
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						cu.showMessageDialog( "コンポーネントの初期化に失敗!!\n" +
								ex.getMessage() );
					}
				} );
			}
		}
		// ウィンドウの準備
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				mMainPanel = new JPanel( new BorderLayout() );
				mMainFrame.getContentPane().removeAll();
				mMainFrame.getContentPane().add( mMainPanel );
				mMainFrame.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
				mComponentSelectPanel = new ComponentSelectPanel( 
						mPanelManagingComponentNameList.toArray( new String[0] ) );
				mMainPanel.add( mComponentSelectPanel, BorderLayout.NORTH );
				mMainFrame.validate();
				mMainFrame.repaint();
			}
		} );
		// 最初のパネルを表示しておく
		changeProcessingComponent( mPanelManagingComponentNameList.get( 0 ) );
	}
	
	void stop() {
		// イベントディスパッチスレッドから呼んではいけない
		assert ! SwingUtilities.isEventDispatchThread();
		// ウィンドウの終了準備
		// TODO : "終了中です" の表示, 子コンポーネントの取り外し
		//showFinalPanel();
		changeProcessingComponent( null );
		mMainFrame.getContentPane().removeAll();
		mMainFrame.getContentPane().add( new TextPanel( "終了処理中です...", 200 ) );
		mMainFrame.validate();
		mMainFrame.repaint();
		
		// コンポーネントの破棄
		for( AppComponent comp : mComponentList ) {
			comp.dispose();
		}
		//mInitProcessingComponent.dispose();
		
		// ウィンドウ破棄
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				//mMainPanel.remove( mFinalProcessingComponent.getPanel() );
				//mFinalProcessingComponent.dispose();
				mMainFrame.dispose();
			}
		} );
	}
	
	// このメソッド内で sleep する可能性あり
	// null を渡すと除去
	boolean changeProcessingComponent( String componentName ) {
		// イベントディスパッチスレッドから呼んではいけない
		assert ! SwingUtilities.isEventDispatchThread();
		// TODO : mActiveProcessingComponent のマルチスレッド対応?
		AppComponentPanelManager newPC = null;
		if( componentName != null ) {
			newPC = mPanelManagingComponentMap.get( componentName );
			if( newPC == null ) {
				// TODO
				throw new RuntimeException( "指定のコンポーネント [" + componentName + "] は存在しません..." );
			}
		}
		return changeComponentPanel( newPC, componentName );
	}
	
	// このメソッド内で sleep する可能性あり
	boolean changeComponentPanel( final AppComponentPanelManager component, final String componentName ) {
		// イベントディスパッチスレッドから呼んではいけない
		assert ! SwingUtilities.isEventDispatchThread();
		// TODO : mActiveProcessingComponent のマルチスレッド対応?
		boolean doChange = true;
		if( mActiveProcessingComponent != null ) {
			// もともとのコンポーネントを除去して良い?
			if( ! mActiveProcessingComponent.hidden() ) {
				doChange = false;
			}
		}
		if( doChange ) {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					if( mActiveProcessingComponent != null ) {
						mMainPanel.remove( mActiveProcessingComponent.getPanel() );
					}
					if( component != null ) {
						mComponentSelectPanel.changeSelectedItem( componentName );
						mActiveProcessingComponent = component;
						JPanel newPanel = mActiveProcessingComponent.getPanel();
						mMainPanel.add( newPanel, BorderLayout.CENTER );
					}
					mMainFrame.validate();
					mMainFrame.repaint();
				}
			} );
		}
		if( component != null ) {
			component.show();
		}
		return doChange;
	}
	
}
