package com.lzw.widget;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.lzw.library4Refreshlistview.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;



/**
 * @author andong
 * ����ˢ��, �������� ListView
 */
public class RefreshListView extends ListView implements OnScrollListener {

	private final String TAG = "RefreshListView";
	private int firstVisibleItem;	// ����ʱ��ʾ�ڶ�����position
	private int downY = -1; // ����y���ƫ����
	private int refreshViewHeight;	// ����ˢ�²��ֵĸ߶�
	private LinearLayout headerView;	// ͷ����
	private View mRefreshView;	// ͷ����
	private DisplayMode currentState = DisplayMode.PULL_DOWN_REFRESH;	// ͷ���ֵ�ǰ��״̬, Ĭ��Ϊ����״̬
	private RotateAnimation upAnimation;		// ����ת���Ķ���
	private RotateAnimation downAnimation;		// ����ת���Ķ���
	private ImageView ivArrow;	// ��ͷ
	private ProgressBar pbProgress;	// ������
	private TextView tvState;	// ˢ��״̬
	private TextView tvLastUpdateTime;	// ���ˢ��ʱ��
	private OnRefreshListener mOnRefreshListener;	// ����ˢ�¼����¼�
	private boolean isScroll2Bottom = false;	// �Ƿ�������ײ�
	private View footerView; // ���ظ��಼��
	private int footerViewHeight; // ���ظ��಼�ָ߶�
	private boolean isLoadMoring = false;	// �Ƿ����ڼ��ظ�����
	private boolean isEnablePullToRefresh = false;	// �Ƿ���������ˢ�¹���
	private boolean isEnableLoadingMore = false;	// �Ƿ����ü��ظ��๦��
	private View topView;
	private int mLocationInWindowY = -1;

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		initHeaderView();
		initFooterView();
		setOnScrollListener(this);
	}

	public void addCustomHeaderView(View v) {
		this.topView = v;
		headerView.addView(v);
	}
	
	/**
	 * ��ʼ��ͷ����
	 */
	private void initHeaderView() {
		headerView = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.listview_header, null);
		mRefreshView = headerView.findViewById(R.id.listview_header);
		
		ivArrow = (ImageView) mRefreshView.findViewById(R.id.iv_listview_header_arrow);
		pbProgress = (ProgressBar) mRefreshView.findViewById(R.id.pb_progress);
		tvState = (TextView) mRefreshView.findViewById(R.id.tv_refresh_state);
		tvLastUpdateTime = (TextView) mRefreshView.findViewById(R.id.tv_last_update_time);
		
		ivArrow.setMinimumWidth(50);
		tvLastUpdateTime.setText("���ˢ��ʱ��: " + getLastUpdateTime());
		
		mRefreshView.measure(0, 0);
		refreshViewHeight = mRefreshView.getMeasuredHeight();
		
		mRefreshView.setPadding(0, -refreshViewHeight, 0, 0);
		
		addHeaderView(headerView);
		initAnimation();
	}
	
	/**
	 * ��ʼ������
	 */
	private void initAnimation() {
		upAnimation = new RotateAnimation(
				0f, -180f, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnimation.setDuration(500);
		upAnimation.setFillAfter(true);
		
		downAnimation = new RotateAnimation(
				-180f, -360f, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnimation.setDuration(500);
		downAnimation.setFillAfter(true);
	}
	
	/**
	 * ��ʼ���Ų���
	 */
	private void initFooterView() {
		footerView = LayoutInflater.from(getContext()).inflate(R.layout.listview_footer, null);
		footerView.measure(0, 0);
		footerViewHeight = footerView.getMeasuredHeight();
		
		// ���ؽŲ���
		footerView.setPadding(0, -footerViewHeight, 0, 0);
		addFooterView(footerView);
	}

	/**
	 * ������µ�ʱ��
	 * @return
	 */
	private String getLastUpdateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}
	
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
		/**
		 * SCROLL_STATE_IDLE ������ͣ��ʱ����
		 * SCROLL_STATE_TOUCH_SCROLL ��ס��ĻʹListView����ʱ����
		 * SCROLL_STATE_FLING ���ٵ��϶���Ļ��������
		 */
		if(isEnableLoadingMore
				&&(scrollState == OnScrollListener.SCROLL_STATE_IDLE || scrollState == OnScrollListener.SCROLL_STATE_FLING)
				&& isScroll2Bottom
				&& !isLoadMoring) {
			footerView.setPadding(0, 0, 0, 0);
			setSelection(getCount());	// ������ListView�����һ��
			isLoadMoring = true;
			// �ص��û��ļ����¼�
			if(mOnRefreshListener != null) {
				mOnRefreshListener.onLoadMoreData();
			}
		}
	}

	/**
	 * ��ListView����ʱ�ص�
	 * firstVisibleItem ��ǰ��Ļ����ʱ�����ʾ��item��position
	 * visibleItemCount ��ǰ��Ļ����ʱ��ʾ���ٸ���Ŀ
	 * totalItemCount ListView��������
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		this.firstVisibleItem = firstVisibleItem;
		
		if((firstVisibleItem + visibleItemCount) == totalItemCount
				&& totalItemCount > visibleItemCount) {
			isScroll2Bottom = true;
		} else {
			isScroll2Bottom = false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			if(downY == -1) {
				downY = (int) ev.getY();
			}
			
			if(topView != null) {
				int[] location = new int[2];
				if(mLocationInWindowY == -1) {
					getLocationInWindow(location);
					mLocationInWindowY = location[1];
				}
				
				topView.getLocationInWindow(location);
				if(location[1] < mLocationInWindowY) {
					break;
				}
			}

			int moveY = (int) ev.getY();	// �ƶ��е�y��ƫ����

			int paddingTop = -refreshViewHeight + (moveY - downY) / 2;
			
			if(currentState == DisplayMode.REFRESHING) {	// �������ˢ����, ���������Ķ���
				break;
			}
			
			if(isEnablePullToRefresh
					&& firstVisibleItem == 0
					&& paddingTop > -refreshViewHeight) {
				
				if(currentState == DisplayMode.PULL_DOWN_REFRESH
						&& paddingTop > 0) {		// ��ȫ��ʾ,���ҵ�ǰ��״̬������ˢ��״̬
					currentState = DisplayMode.RELEASE_REFRESH;
					refreshHeaderViewState();
				} else if(currentState == DisplayMode.RELEASE_REFRESH
						&& paddingTop < 0) {	// û����ȫ��ʾ, ���ҵ�ǰ��״̬���ɿ�ˢ��״̬
					currentState = DisplayMode.PULL_DOWN_REFRESH;
					refreshHeaderViewState();
				}
				mRefreshView.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			downY = -1;
			
			if(currentState == DisplayMode.PULL_DOWN_REFRESH) {	// ����ˢ��״̬
				mRefreshView.setPadding(0, -refreshViewHeight, 0, 0);
			} else if(currentState == DisplayMode.RELEASE_REFRESH) {	// �ɿ�ˢ��
				mRefreshView.setPadding(0, 0, 0, 0);
				currentState = DisplayMode.REFRESHING;
				refreshHeaderViewState();
				
				if(mOnRefreshListener != null) {
					// ֪ͨ�û�ˢ������
					mOnRefreshListener.onRefresh();
				}
			}
			break;
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	/**
	 * ��ˢ�����ʱ�ص�
	 */
	public void onRefreshFinish() {
		if(isLoadMoring) {
			isLoadMoring = false;
			footerView.setPadding(0, -footerViewHeight, 0, 0);
		} else {
			mRefreshView.setPadding(0, -refreshViewHeight, 0, 0);
			currentState = DisplayMode.PULL_DOWN_REFRESH;
			ivArrow.setVisibility(View.VISIBLE);
			pbProgress.setVisibility(View.GONE);
			tvLastUpdateTime.setText("���ˢ��ʱ��" + getLastUpdateTime());
		}
	}
	
	/**
	 * ˢ��ͷ���ֵ�״̬
	 */
	private void refreshHeaderViewState() {
		if(currentState == DisplayMode.PULL_DOWN_REFRESH) {	// ����ˢ��״̬
			ivArrow.startAnimation(downAnimation);
			tvState.setText("����ˢ��");
		} else if(currentState == DisplayMode.RELEASE_REFRESH) {	// �ɿ�ˢ��
			ivArrow.startAnimation(upAnimation);
			tvState.setText("�ɿ�ˢ��");
		} else if(currentState == DisplayMode.REFRESHING) {		// ����ˢ��
			ivArrow.clearAnimation();
			ivArrow.setVisibility(View.GONE);
			pbProgress.setVisibility(View.VISIBLE);
			tvState.setText("����ˢ��");
		}
	}
	
	/**
	 * @author andong
	 * ͷ���ֵ�״̬
	 */
	public enum DisplayMode {
		PULL_DOWN_REFRESH,	// ����ˢ��
		RELEASE_REFRESH,	// �ɿ�ˢ��
		REFRESHING			// ����ˢ����
	}
	
	/**
	 * ����ˢ���¼�
	 * @param listener
	 */
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}
	
	/**
	 * ��������ˢ�µĿ���
	 * @param isEnablePullToRefresh true:����
	 */
	public void setPullToRefreshEnable(boolean isEnablePullToRefresh) {
		this.isEnablePullToRefresh = isEnablePullToRefresh;
	}
	
	/**
	 * ���ü��ظ���Ŀ���
	 * @param isEnableLoadingMore true:����
	 */
	public void setLoadingMoreEnable(boolean isEnableLoadingMore) {
		this.isEnableLoadingMore = isEnableLoadingMore;
	}
}