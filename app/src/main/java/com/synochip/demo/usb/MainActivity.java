package com.synochip.demo.usb;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.util.Random;

import com.synochip.demo.usb.R;
//import com.synochip.OTG_Demo.R;
//import com.synochip.demo.OTG_KEY.R;
import com.synochip.sdk.ukey.Tool;
import com.synochip.sdk.ukey.OTG_KEY;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.R.bool;
import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.app.PendingIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;


public class MainActivity extends  Activity implements View.OnClickListener {

	private static final String ACTION_USB_PERMISSION = "com.synochip.demo.OTG_DEMO";
	
	public int threadCnt;
	
	private static final int MAX_LINES = 12;
	private static final int CNT_LINES = 11;
	
	private static final int PS_NO_FINGER = 0x02;
	private static final int PS_OK = 0x00;
	private static int CHAR_BUFFER_A = 0x01;
	private static int CHAR_BUFFER_B = 0x02;
	private final static int DEV_ADDR = 0xffffffff;
	private static int fingerCnt = 1;
	private static int IMAGE_X = 256;
	private static int IMAGE_Y = 288;
	
	//private int opened = 0;
	public int thread_i = 0;
	public int thread_sum = 0;
	private UsbManager mUsbManager;
	private UsbDevice mDevice;
	private PendingIntent mPermissionIntent;

	private int MAX_ENROLL = 2;

	boolean globalControl = true;
	
	private OTG_KEY msyUsbKey;
	int mhKey =0;
	int mhCon =0;
	private TextView mResponseTextView;
	public TextView mImputTextView;
	private Button mOpen;
	private Button mClose;
	private Button mSearch;
	private Button mClear;
	private Button mDevMsg;
	private Button mUpImage;
	private Button mEnroll;
	private Button mStop;
	private Button mUpChar;
	private Button mDownChar;
	
	String imagePath = "finger.bmp";
	byte[] fingerBuf = new byte[IMAGE_X*IMAGE_Y];
	//private EditText mLongEdit;
	ProgressBar bar = null;
	boolean ifChecked = false;
	
	//private EditText mEditText;
	ImageView fingerView = null;
	
	byte mbAppHand[]=new byte[1];
	byte mbConHand[]=new byte[1];
	boolean bIsOpen=false;
	byte[] g_TempData = new byte[512];
	public boolean start_clt = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
  
        mOpen = (Button) findViewById(R.id.BOpen);
		mOpen.setOnClickListener(this);

		mClose = (Button) findViewById(R.id.BClose);
		mClose.setOnClickListener(this);
		
		mDevMsg = (Button) findViewById(R.id.devMsg);
		mDevMsg.setOnClickListener(this);
		
		mUpImage = (Button)findViewById(R.id.upImage);
		mUpImage.setOnClickListener(this);
		
		bar = (ProgressBar)findViewById(R.id.bar);
		fingerView = (ImageView)findViewById(R.id.fingerImage);
		
		mEnroll = (Button) findViewById(R.id.BTEnroll);
		mEnroll.setOnClickListener(this);
			
		mSearch = (Button) findViewById(R.id.BSearch);
		mSearch.setOnClickListener(this);
		
		mClear = (Button) findViewById(R.id.BTClear);
		mClear.setOnClickListener(this);
	
		mStop = (Button)findViewById(R.id.BStop);
		mStop.setOnClickListener(this);
		
		mUpChar = (Button)findViewById(R.id.upChar);
		mUpChar.setOnClickListener(this);
		
		mDownChar = (Button)findViewById(R.id.downChar);
		mDownChar.setOnClickListener(this);
		
		mImputTextView = (TextView)findViewById(R.id.imputTextView);
		mResponseTextView = (TextView) findViewById(R.id.TVLog);
		mResponseTextView.setMovementMethod(new ScrollingMovementMethod());
		//mResponseTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
		//mResponseTextView.setLines(17);
		mResponseTextView.setMaxLines(MAX_LINES);
		mResponseTextView.setText("");

	    //mEditText = (EditText)findViewById(R.id.ETSend);
	    //mEditText.addTextChangedListener(mTextWatcher);

    	mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);// 启动服务进程
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		
		openState(true);
		registerReceiver(mUsbReceiver, filter);
		
    }
   
    //pause
    protected void onPause(){
    	super.onPause();
    	if(start_clt)
    	{
    		logMsg("onPause onPause onPause");
    		globalControl = false;
    		ctlStatus(false);
    	}
    }
    
    public void onClick(View v) {
    
    	if (v == mOpen) {
			boolean requested = false;
			
				for (UsbDevice device : mUsbManager.getDeviceList().values()) {
					if (0x2109 == device.getVendorId()  && 0x7638 == device.getProductId() )
					{
						mDevice = device;
						mUsbManager.requestPermission(mDevice, mPermissionIntent);
						requested = true;
						logMsg("find this usb device vID:0x2109");
						break;
					}
					if (0x0453 == device.getVendorId()  && 0x9005 == device.getProductId() )
					{
						mDevice = device;
						mUsbManager.requestPermission(mDevice, mPermissionIntent);
						requested = true;
						logMsg("find this usb device vID:0x0453");
						break;
					}
				}
			
			if (requested) {
				if(!mUsbManager.hasPermission(mDevice)){
					logMsg("no Permission!");
					return;
				}
					try {
						msyUsbKey = null;
						msyUsbKey = new OTG_KEY(mUsbManager,mDevice);
						int key[] = new int[1];
						int ret = msyUsbKey.UsbOpen();
						if( ret == OTG_KEY.DEVICE_SUCCESS )
						{
							mhKey = key[0];
							logMsg("open device success hkey :"+Tool.int2HexStr(mhKey));
						}
						else
						{
							logMsg("open device fail errocde :"+ ret);//Tool.int2HexStr(ret));
						}
						
					} catch (Exception e) {
						logMsg("Exception: => " + e.toString());	
						return;
					}
				int ret;
				System.out.println("start password ...");
				if( (ret = msyUsbKey.PSword()) != 0)
				{
					logMsg("密码验证错误"+ ret);
					System.out.println("密码验证错误");
					//logMsg("my return:"+ msyUsbKey.synoprintf());
					return;
				}
				System.out.println("password��֤�ɹ�");
			} else {
				logMsg("can't find this device!");
				return ;
			}
			//获取设备信息
			int[] indexMax = new int[1];
			int[] len = new int[1];
			byte[] index = new byte[256];
			if( 0 != getUserContent( indexMax, index, len) )
			{
				logMsg("获取设备信息失败");
				return;
			}
			if( len[0] == 0 )
			{
				logMsg("从第"+indexMax[0]+"个ID开始存储指纹");
				fingerCnt = 0;
			}
			else {
			indexMax[0]++;
			logMsg("从第"+indexMax[0]+"个ID开始存储指纹");
			fingerCnt = indexMax[0];
			}
			openState(false);
			start_clt = true;
    	} else if (v == mClose) {
			try {
				//uiState(true);
				logMsg("设备已关闭");
				openState(true);
				msyUsbKey.CloseCard(mhKey);
			} catch (Exception e) {
				logMsg("Exception: => " + e.toString());
				return;
			}
		}
		else if (v == mEnroll) { // finger
			
			//logMsg("请放上手指ָ");
			//bar.setVisibility(View.VISIBLE);
			//bar.setProgress(0);
			//handler.post(mGetFingerThread);
			
			if( fingerCnt >= 256)
			{
				logMsg("指纹库已满，请删除");
				return;
			}
			ctlStatus(true);
			globalControl = true;
			bar.setVisibility(View.VISIBLE);
			bar.setProgress(0);
			ImputAsyncTask asyncTask = new ImputAsyncTask();
			asyncTask.execute(1);
		}
		else if( v == mSearch ) //搜索指纹
		{
			bar.setVisibility(View.INVISIBLE);
			 globalControl = true;
			 ctlStatus(true);
			 SearchAsyncTask asyncTask_search = new SearchAsyncTask();
			 asyncTask_search.execute(1);
		}
		else if ( v == mClear ) //清空指纹库
		{
			if( PS_OK != msyUsbKey.PSEmpty(DEV_ADDR) )
			{
				logMsg("清空指纹库失败");
			}
			fingerCnt = 0;
			logMsg("清空指纹库成功");
		}
		else if( v == mDevMsg )
		{
			int[] indexMax = new int[1];
			int[] len = new int[1];
			byte[] index = new byte[256];
			if( 0 != getUserContent( indexMax, index, len) )
			{
				logMsg("获取设备信息失败");
				return;
			}
			
			logMsg("目前设备存储有"+len[0]+"个指纹信息");
			
			//byte[] temp = new byte[len[0]];
			int i;
			logMsg("他们分别是:");
			for( i=0; i<len[0];i++)
			{
				logMsg("id:"+index[i]);
			}
			if( len[0] != 0)
			{
				logMsg("最大的ID为"+indexMax[0]);
			}
			logMsg("获取设备信息成功");
		}
		else if( v == mUpImage ) //上传图像
		{
			int ret;
			ctlStatus(true);
			globalControl = true;
			UpAsyncTask asyncTask_up = new UpAsyncTask();
			asyncTask_up.execute(1);
		}
		else if ( v == mStop ) {
			globalControl = false;
			ctlStatus(false);
		}
		else if ( v == mUpChar ) {
			//test id need you set
			int pageId = 0;
			if( 0 != msyUsbKey.loadChar( CHAR_BUFFER_A, pageId ))
			{
				logMsg("loadChar失败");
				return;
			}
			if(0 == msyUsbKey.upChar( CHAR_BUFFER_A, g_TempData, 512))
			{
				logMsg(  "g_TempData upChar = "+bytesToHexString(g_TempData));
				logMsg("上传特征成功");
			}
			else {
				logMsg("上传特征失败");
			}
		}
		else if ( v == mDownChar ) {
			//the 8 is test value
			//test id need you set
			int pageId = 8;
			logMsg(  "g_TempData downChar = "+bytesToHexString(g_TempData));
			if( 0 == msyUsbKey.downChar( CHAR_BUFFER_A, g_TempData, 512))
			{
				logMsg("下载特征成功");
			}
			else {
				logMsg("下载特征失败");
				return;
			}
			try {
				//pageId由你决定，和录入一样的,这个ID由你决定，这里必须存储，不然将搜索不到
				if( msyUsbKey.storeChar(CHAR_BUFFER_A, pageId) != PS_OK)
				{	
					logMsg("存储模板失败");
					return;
				}
				else {
					logMsg("存储模板成功");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.menu_settings:
					set_max_enroll_cnt();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	public static final String bytesToHexString(byte[] bArray) {
		  StringBuffer sb = new StringBuffer(bArray.length);
		  String sTemp;
		  for (int i = 0; i < bArray.length; i++) {
		   sTemp = Integer.toHexString(0xFF & bArray[i]);
		   if (sTemp.length() < 2)
		    sb.append(0);
		   sb.append(sTemp.toUpperCase());
		  }
		  return sb.toString();
		 }
    
 // 捕获usb的插拔消息
 	private  final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
 		//收到消息
 		public void onReceive(Context context, Intent intent) {

 			String action = intent.getAction();
 			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
 				UsbDevice device = (UsbDevice) intent
 						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
 				if(device != null){
                     //call method to set up device communication
                  }
 				logMsg("Add:  DeviceName:  " + device.getDeviceName()
 						+ "  DeviceProtocol: " + device.getDeviceProtocol()
 						+ "\n");

 			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
 				UsbDevice device = intent
 						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
 				logMsg("Del: DeviceName:  " + device.getDeviceName()
 						+ "  DeviceProtocol: " + device.getDeviceProtocol()
 						+ "\n");
 				openState(true);
 				start_clt = false;
 				if(null != msyUsbKey){
 					msyUsbKey.CloseCard(mhKey);
 					msyUsbKey = null;
 				}
 			}
 		}
 	};
 	
 	public  synchronized void  logMsg(String msg) {
		String oldMsg = mResponseTextView.getText().toString();

		mResponseTextView.setText(oldMsg + "\n"
				+msg);
		new myAsyncTask().execute();
	}
 	
 	/*
 	private final TextWatcher mTextWatcher = new TextWatcher() {
 	    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
 	    } 

 	    public void onTextChanged(CharSequence s, int start, int before, int count) {
 	    } 

 	    public void afterTextChanged(Editable s) {
 	        if (s.length() > 0) {
 	            int pos = s.length() - 1;
 	            char c = s.charAt(pos);
 	            if ( !(  c >= '0'&&c <='9'|| c >= 'a'&&c <='f' || c >= 'A'&&c <='F')) {
 	                s.delete(pos,pos+1);
 	                Toast.makeText(MainActivity.this, "Error letter.",Toast.LENGTH_SHORT).show();
 	            }
 	        }
 	    }
 	};
 	*/
	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	private class myAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mResponseTextView.getLineCount() > CNT_LINES)
			{
				//Toast.makeText(getApplicationContext(), "LINE:"+mResponseTextView.getLineCount()+" CNT", Toast.LENGTH_SHORT).show();
				mResponseTextView.scrollTo(0,
						(mResponseTextView.getLineCount() - CNT_LINES)
								* mResponseTextView.getLineHeight()+5);
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			return null;
		}
	}
	
	//imput finger thread
	Handler handler = new Handler(){
		public void handleMessage(Message msg){
			bar.setProgress(msg.arg1);
			if( 0 == msg.arg1)
			{
				handler.post(mGetFingerThread);
			}
			else if( 50 == msg.arg1)
			{
				logMsg("第一枚手指获取成功");
				logMsg("请放入第二枚手指ָ");
				
				int ret;
				if( (ret= msyUsbKey.PSUpImage(DEV_ADDR, fingerBuf))!=0)
				{
					logMsg("上传图像失败:"+ret);
				}
				//logMsg("上传图像成功");
				if( (ret = WriteBmp(fingerBuf)) != 0)
				{
					logMsg("生成bmp文件失败:"+ret);
				}
				//logMsg("图像写入成功");
				String localName = "finger.bmp";
				FileInputStream localStream = null;
				try {
					localStream = openFileInput(localName);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					try {
						localStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
				Bitmap bitmap = BitmapFactory.decodeStream(localStream);
				fingerView.setImageBitmap(bitmap);
				
				try {
					localStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				handler.post(mGetFingerThread);
			}
			else if( 100 == msg.arg1)
			{
				logMsg("第二枚手指获取成功");
				logMsg("ָ指纹存储成功");
			}
		}
	};
	
	public class UpAsyncTask extends AsyncTask<Integer, String, Integer>
	{
		@Override
		protected Integer doInBackground(Integer... params) {
			int ret = 0;
			while( true )
			{
				if( globalControl == false )
                {
                    return -1;
                }
				while( msyUsbKey.PSGetImage(DEV_ADDR) != PS_NO_FINGER )
				{
					if( globalControl == false )
	                {
	                    return -1;
	                }
					try {
						Thread.sleep(20);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				while( msyUsbKey.PSGetImage(DEV_ADDR) == PS_NO_FINGER )
				{
					if( globalControl == false )
	                {
	                    return -1;
	                }
					try {
						Thread.sleep(10);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				
				if( (ret= msyUsbKey.PSUpImage(DEV_ADDR, fingerBuf))!=0)
				{
					publishProgress("上传图像失败:"+ret);
					continue;
				}
				//logMsg("上传图像成功");
				if( (ret = WriteBmp(fingerBuf)) != 0)
				{
					publishProgress("生成bmp文件失败:"+ret);
					continue;
				}
				//logMsg("图像写入成功");
				publishProgress("OK");
			}
			// TODO Auto-generated method stub
		}
		//线程结束
		protected void onPostExecute( Integer result)
		{
			return;
		}
		
		//线程开始
		protected void onPreExecute()
		{
			logMsg("显示图片,请在传感器放上手指ָ");
			return;
		}
		
		//线程中间状态
		protected void onProgressUpdate( String... values )
		{
			if( values[0].equals("OK") )
			{
				String localName = "finger.bmp";
				FileInputStream localStream = null;
				try {
					localStream = openFileInput(localName);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					try {
						localStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
				Bitmap bitmap = BitmapFactory.decodeStream(localStream);
				fingerView.setImageBitmap(bitmap);
				try {
					localStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			logMsg(values[0]);
			mImputTextView.setText(values[0]);
			//Toast.makeText(getApplicationContext(), values[0], Toast.LENGTH_SHORT).show();
			return;
		}
	}
	
	public class SearchAsyncTask extends AsyncTask<Integer, String, Integer>
	{
		@Override
		protected Integer doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int ret;
			int[] fingerId = new int[1];
			while(true)
			{
				if( globalControl == false )
                {
                    return -1;
                }
				while( msyUsbKey.PSGetImage(DEV_ADDR) == PS_NO_FINGER )
				{
					if( globalControl == false )
	                {
	                    return -1;
	                }
					try {
						Thread.sleep(20);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				if( (ret= msyUsbKey.PSUpImage(DEV_ADDR, fingerBuf))!=0)
				{
					//publishProgress("上传图像失败:"+ret);
					continue;
				}
				//logMsg("上传图像成功");
				if( (ret = WriteBmp(fingerBuf)) != 0)
				{
					//publishProgress("生成bmp文件失败:"+ret);
					continue;
				}
				//logMsg("图像写入成功");
				publishProgress("OK");
				
				if( msyUsbKey.PSGenChar(DEV_ADDR, CHAR_BUFFER_A)!= PS_OK )
				{
					//publishProgress("生成特征失败");
					continue;
				}
				if( PS_OK != msyUsbKey.PSSearch(DEV_ADDR, CHAR_BUFFER_A, 0, 10, fingerId))
				{
					publishProgress("没有找到此指纹");
					continue;
				}
				publishProgress("成功搜索到此指纹,ID===>"+fingerId[0]);
			}
		}
		//线程结束
		protected void onPostExecute( Integer result)
		{
			return;
		}
		
		//线程开始
		protected void onPreExecute()
		{
			logMsg("搜索指纹=>开始,请放上手指ָ");
			return;
		}
		
		//线程中间状态
		protected void onProgressUpdate( String... values )
		{
			if( values[0].equals("OK") )
			{
				String localName = "finger.bmp";
				FileInputStream localStream = null;
				try {
					localStream = openFileInput(localName);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					try {
						localStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
				Bitmap bitmap = BitmapFactory.decodeStream(localStream);
				fingerView.setImageBitmap(bitmap);
				try {
					localStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			logMsg(values[0]);
			mImputTextView.setText(values[0]);
			//Toast.makeText(getApplicationContext(), values[0], Toast.LENGTH_SHORT).show();
			return;
		}
	}
			
			
	public class ImputAsyncTask extends AsyncTask<Integer, String, Integer>
	{
		byte[][] fingerFeature = new byte[6][512];
		int Progress = 0;
		
		@Override
		protected Integer doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int cnt = 1;
			int ret;
			while( true )
			{
				if( globalControl == false )
                {
                    return -1;
                }
				while( msyUsbKey.PSGetImage(DEV_ADDR) != PS_NO_FINGER )
				{
					if( globalControl == false )
	                {
	                    return -1;
	                }
					try {
						Thread.sleep(20);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				while( msyUsbKey.PSGetImage(DEV_ADDR) == PS_NO_FINGER )
				{
					if( globalControl == false )
	                {
	                    return -1;
	                }
					try {
						Thread.sleep(10);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				
				if( (ret= msyUsbKey.PSUpImage(DEV_ADDR, fingerBuf))!=0)
				{
					publishProgress("上传图像失败:"+ret);
					continue;
				}
				//logMsg("上传图像成功");
				if( (ret = WriteBmp(fingerBuf)) != 0)
				{
					publishProgress("生成bmp文件失败:"+ret);
					continue;
				}
				//logMsg("图像写入成功");
				publishProgress("OK");
				
				//生成模板
				if( cnt < MAX_ENROLL)
				{
					if( (ret = msyUsbKey.PSGenChar(DEV_ADDR, CHAR_BUFFER_A)) != PS_OK )
					{
						publishProgress("ָ指纹"+ cnt +"生成特征失败:"+ret);
						return -1;
					}
					else {
						publishProgress("请再次放上手指ָ");
						//publishProgress("生成模板失败");
					}
				}
				if( cnt == MAX_ENROLL)
				{
					if( (ret = msyUsbKey.PSGenChar(DEV_ADDR, CHAR_BUFFER_B)) != PS_OK )
					{
						publishProgress("ָ指纹"+cnt+"生成特征失败:"+ret);
						continue;
					}
					else {
						//publishProgress("生成模板失败");
					}
					if( msyUsbKey.PSRegModule(DEV_ADDR) != PS_OK )
					{
						//bar.setProgress(0);
						thread_i = 0;
						thread_sum = 0;
						publishProgress("生成模板失败，请重新录入");
						//handler.removeCallbacks(mGetFingerThread);
						return -1;
					}
					//publishProgress("生成模板成功");
					//need to fix the 1
					if( fingerCnt >= 256 )
					{
						publishProgress("模板存满，请删除部分指纹信息");
					}
					if( msyUsbKey.PSStoreChar(DEV_ADDR, 1, fingerCnt) != PS_OK)
					{
						//bar.setProgress(0);
						thread_i = 0;
						thread_sum = 0;
						publishProgress("存储特征失败,请重新录入");
						//handler.removeCallbacks(mGetFingerThread);
						return -1;
					}
					publishProgress("录入指纹成功,=====>ID:"+fingerCnt);
					return 0;
				}
				cnt++;
			}
			//publishProgress("ָ指纹录入成功");
		}
		
		//线程结束
		protected void onPostExecute( Integer result)
		{
			globalControl = false;
			ctlStatus(false);
			if(0 == result )
			{
				if( fingerCnt > 256)
				{
					logMsg("ָ指纹存储256个手指已满，请删除指纹数据");
					return;
				}
				//logMsg("录入指纹成功");
				//logMsg("fingerFeature[0]:"+Base64.encodeToString(fingerFeature[0], Base64.DEFAULT ));
				fingerCnt++;
				bar.setProgress(100);
				return;
			}
			else {
				bar.setProgress(0);
				logMsg("指纹录入失败，请重新录入");
				return;
			}
		}
		
		//线程开始
		protected void onPreExecute()
		{
			logMsg("录入指纹=>开始,请放上手指ָ");
			mImputTextView.setText("录入指纹=>开始,请放上手指ָ");
			return;
		}
		
		//线程中间状态
		protected void onProgressUpdate( String... values )
		{
			if( values[0].equals("OK") )
			{
				String localName = "finger.bmp";
				FileInputStream localStream = null;
				try {
					localStream = openFileInput(localName);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					try {
						localStream.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
				Bitmap bitmap = BitmapFactory.decodeStream(localStream);
				fingerView.setImageBitmap(bitmap);
				Progress += 100/MAX_ENROLL;
				bar.setProgress(Progress);
				try {
					localStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			logMsg(values[0]);
			//Toast.makeText(getApplicationContext(), values[0], Toast.LENGTH_SHORT).show();
			return;
		}
	}
	
	
	Runnable mGetFingerThread = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = handler.obtainMessage();
			int ret;
			if( thread_i == 0 )
			{
				msg.arg1 = 0;
				handler.sendMessage(msg);
			}
			else if( thread_i == 1 )
			{
				while( msyUsbKey.PSGetImage(DEV_ADDR) == PS_NO_FINGER )
				{
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				try {
					Thread.sleep(30);
				} catch (Exception e) {
					// TODO: handle exception
				}
				if( (ret = msyUsbKey.PSGenChar(DEV_ADDR, CHAR_BUFFER_A)) != PS_OK )
				{
					logMsg("ָ指纹1生成特征失败:"+ret);
				}
				else
				{
					logMsg("ָ指纹1生成特征成功");
				}
				thread_sum += 50;
				msg.arg1 = thread_sum;
				handler.sendMessage(msg);
			}
			else if( thread_i == 2 )
			{
				while(true)
				{
					if( msyUsbKey.PSGetImage(DEV_ADDR) != PS_NO_FINGER )
					{
						break;
					}
					try {
						Thread.sleep(30);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				logMsg("请第二次放入相同手指ָ");
				
				while( msyUsbKey.PSGetImage(DEV_ADDR) == PS_NO_FINGER )
				{
					try {
						Thread.sleep(30);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				logMsg("获取第二个指纹成功");
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					// TODO: handle exception
				}
				if( msyUsbKey.PSGenChar(DEV_ADDR, CHAR_BUFFER_B) != PS_OK )
				{
					bar.setProgress(0);
					thread_i = 0;
					thread_sum = 0;
					logMsg("第二枚指纹获取特征失败，请重新录入");
					//handler.removeCallbacks(mGetFingerThread);
					return;
				}
				else {
					logMsg("第二枚指纹获取特征成功");
				}
				thread_sum += 50;
				msg.arg1 = thread_sum;
				//handler.sendMessage(msg);
				if( msyUsbKey.PSRegModule(DEV_ADDR) != PS_OK )
				{
					bar.setProgress(0);
					thread_i = 0;
					thread_sum = 0;
					logMsg("生成模板失败，请重新录入");
					//handler.removeCallbacks(mGetFingerThread);
					return;
				}
				//logMsg("生成模板成功");
				//need to fix the 1
				if( fingerCnt >= 256 )
				{
					logMsg("模板存满，请删除部分指纹信息");
				}
				if( msyUsbKey.PSStoreChar(DEV_ADDR, 1, fingerCnt) != PS_OK)
				{
					bar.setProgress(0);
					thread_i = 0;
					thread_sum = 0;
					logMsg("存储特征失败,请重新录入");
					//handler.removeCallbacks(mGetFingerThread);
					return;
				}
				logMsg("录入指纹成功,=====>ID:"+fingerCnt);
				fingerCnt++;
				bar.setProgress(thread_sum);
				thread_i = 0;
				thread_sum = 0;
				//handler.removeCallbacks(mGetFingerThread);
			}
			thread_i++;
		}
	};
	
	public int getUserContent(int[] max, byte[] fingerid, int[] len)
	{
		byte[] UserContent = new byte[32];
		byte bt,b;
		int ret = 0;
		int i;
		int iBase;
		int iIndex = 0;
		int iIndexOffset;
		int[] indexFinger = new int[256];
		int j = 0;
		int indexMax = 0;
		ret = msyUsbKey.PSReadIndexTable(DEV_ADDR, 0, UserContent);
		if( ret != 0 )
		{
			return -1;
		}
		
		for( i = 0; i < 32; i++)
		{
			bt = UserContent[i];
			iBase = i*8;
			if( bt == (byte)0x00 )
			{
				continue;
			}
			
			for( b=(byte)0x01,iIndexOffset = 0; iIndexOffset<8; b=(byte) (b<<1),iIndexOffset++ )
			{
				if( 0 == (bt&b))
				{
					continue;
				}
				iIndex = iBase + iIndexOffset;
				indexFinger[j] = iIndex;
				j++;
				if( iIndex > indexMax )
				{
					indexMax = iIndex;
				}
			}
		}
		max[0] = indexMax;
		len[0] = j;
		
		for( i = 0; i < j; i++ )
		{
			fingerid[i] = (byte) indexFinger[i];
		}
		
		return 0;
	}
	
	
	public int WriteBmp(byte[] imput)
	{
		String fileName = "finger.bmp";
		FileOutputStream fout = null;
		try {
			fout = openFileOutput(fileName, MODE_PRIVATE);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			return -100;
		}
		byte[] temp_head = {0x42,0x4d,//file type 
				//0x36,0x6c,0x01,0x00, //file size***
				0x0,0x0,0x0,0x00, //file size***
				0x00,0x00, //reserved
				0x00,0x00,//reserved
				0x36,0x4,0x00,0x00,//head byte***
				//infoheader
				0x28,0x00,0x00,0x00,//struct size
				
				//0x00,0x01,0x00,0x00,//map width*** 
				0x00,0x00,0x0,0x00,//map width*** 
				//0x68,0x01,0x00,0x00,//map height***
				0x00,0x00,0x00,0x00,//map height***
				
				0x01,0x00,//must be 1
				0x08,0x00,//color count
				0x00,0x00,0x00,0x00, //compression
				//0x00,0x68,0x01,0x00,//data size***
				0x00,0x00,0x00,0x00,//data size***
				0x00,0x00,0x00,0x00, //dpix
				0x00,0x00,0x00,0x00, //dpiy
				0x00,0x00,0x00,0x00,//color used
				0x00,0x00,0x00,0x00,//color important
		};
		byte[] head = new byte[1078];
		byte[] newbmp = new byte[1078+IMAGE_X*IMAGE_Y];
		System.arraycopy(temp_head, 0, head, 0, temp_head.length);
		
		
		int i,j;
		long num;
		num=IMAGE_X; head[18]= (byte) (num & 0xFF);
		num=num>>8;  head[19]= (byte) (num & 0xFF);
		num=num>>8;  head[20]= (byte) (num & 0xFF);
		num=num>>8;  head[21]= (byte) (num & 0xFF);

		num=IMAGE_Y; head[22]= (byte) (num & 0xFF);
		num=num>>8;  head[23]= (byte) (num & 0xFF);
		num=num>>8;  head[24]= (byte) (num & 0xFF);
		num=num>>8;  head[25]= (byte) (num & 0xFF);
		
		j=0;
		for (i=54;i<1078;i=i+4)
		{
			head[i]=head[i+1]=head[i+2]=(byte) j; 
			head[i+3]=0;
			j++;
		}
		System.arraycopy(head, 0, newbmp, 0, head.length);
		System.arraycopy(imput, 0, newbmp, 1078, IMAGE_X*IMAGE_Y);
		
			try {
				fout.write(newbmp);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return -101;
			}
		try {
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return -102;
		}
		return 0;
	}
	
	private void openState( boolean state)
	{
		mClose.setEnabled(!state);
		mEnroll.setEnabled(!state);
		mDevMsg.setEnabled(!state);
		mUpImage.setEnabled(!state);
		mClear.setEnabled(!state);
		mSearch.setEnabled(!state);
		mStop.setEnabled(!state);
		mUpChar.setEnabled(!state);
		mDownChar.setEnabled(!state);
	}
	private void ctlStatus( boolean state)
	{
		mClose.setEnabled(!state);
		mEnroll.setEnabled(!state);
		mDevMsg.setEnabled(!state);
		mUpImage.setEnabled(!state);
		mClear.setEnabled(!state);
		mOpen.setEnabled(!state);
		mSearch.setEnabled(!state);
		mUpChar.setEnabled(!state);
		mDownChar.setEnabled(!state);
	}

	private void set_max_enroll_cnt(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("请输入最大录入次数");
		final EditText editText = new EditText(this);
//                editText.setInputType(InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE);
		editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
		editText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
		builder.setView(editText);
		builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				String s = editText.getText().toString();
				//logMsg(s);
				if(!s.isEmpty()){
					MAX_ENROLL = Integer.parseInt(s);
				}
			}
		});
		builder.setNegativeButton("exit",null);
		builder.show();
	}
}
