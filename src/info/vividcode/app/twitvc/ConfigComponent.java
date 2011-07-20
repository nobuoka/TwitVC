package info.vividcode.app.twitvc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class ConfigComponent implements AppComponent, AppComponentPanelManager {
	
	private JPanel mPanel;
	private ComponentUtils mUtils;
	private JCheckBox mShiftNeededCheckBox;
	
	@Override
	public void initialize(ComponentUtils utils, ComponentRegister register) {
		mUtils = utils;
		mPanel = new JPanel();
		mPanel.setLayout( new BoxLayout( mPanel, BoxLayout.Y_AXIS ) );
		JPanel shiftCheckPanel = new JPanel();
		mShiftNeededCheckBox = new JCheckBox( "Status Window 消去に Shift ボタンを必要とする" );
		mShiftNeededCheckBox.setSelected( true );
		shiftCheckPanel.add( mShiftNeededCheckBox );
		mPanel.add( shiftCheckPanel );
		JButton okButton = new JButton( "設定" );
		okButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent evt ) {
				Config.putPreferenceBoolean( 
						"info.vividcode.twitvc.StatusWindowDeleterNeedsShift", 
						mShiftNeededCheckBox.isSelected() );
				new Thread() {
					@Override public void run() {
						mUtils.raiseEvent( new AppEvent( "info.vividcode.twitvc.CHANGE_CONFIG", null ) );
					}
				}.start();
			}
		} );
		mPanel.add( okButton );
		register.registPanelManager( this, "設定パネル" );
	}
	
	@Override
	public void dispose() {
	}
	
	@Override
	public JPanel getPanel() {
		return mPanel;
	}
	
	@Override
	public boolean show() {
		return true;
	}
	
	@Override
	public boolean hidden() {
		// TODO Auto-generated method stub
		return true;
	}
	
}
