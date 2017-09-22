package com.synochip.sdk.ukey;

import com.synochip.demo.usb.MainActivity;

import android.R.integer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

public class UsbBase {

	private UsbManager mUsbManger;
	private UsbDevice mUsbDevice = null;
	public static  UsbDeviceConnection mConnection =null;
	private UsbInterface mIntf;
	private String mProductDescriptor = "";
	public static final int USB_DIR_OUT = 0x00000000;
	public static final int USB_ENDPOINT_XFER_BULK = 0x00000002;
	public static final int USB_ENDPOINT_XFER_INT =  0x00000003;
	public static final int USB_DIR_IN = 0x00000080;
	
	public byte[] printf1 = new byte[128];
	private static  UsbEndpoint mPointIn;
	private static  UsbEndpoint mPointOut;
//	private  UsbEndpoint mPointInterrupt;

	public UsbBase(UsbManager mManager, UsbDevice mDev) {
		mUsbManger = mManager;
		mUsbDevice = mDev;
	}

	public String getProductDescriptor(){
		return mProductDescriptor;
	}
			
	public int close() {
		if(null != mConnection ){
			if(true!=mConnection.releaseInterface(mIntf)){
				;// throw  new IllegalArgumentException("connection releaseInterface error");
			}
			mConnection.close();
			mConnection = null;
			return UsbStatus.RETURN_SUCCESS;
		}
		return UsbStatus.RETURN_ERROR;
	}
	
	public int open(){
		close();

		mIntf = mUsbDevice.getInterface(0);
		int count = mIntf.getEndpointCount();
		System.out.println("endpoint out count = "+count);
		for (int i = 0; i< count; i++){//遍历找到�?��的端�?			
			UsbEndpoint Point = mIntf.getEndpoint(i);
			int nDirection = Point.getDirection();
			
			int nType = Point.getType();
			if( USB_ENDPOINT_XFER_BULK == nType){
				if(USB_DIR_OUT == nDirection ){
					mPointOut = Point;
				}
				else if(USB_DIR_IN == nDirection){
					mPointIn  = Point;
				}
			}
//			else if (USB_ENDPOINT_XFER_INT == nType){
//				if(USB_DIR_IN == nDirection){
//					mPointInterrupt  = Point;
//				}
//			}
		}
		if(null == mPointIn ||  null == mPointOut ){
			return UsbStatus.RETURN_ERROR;
		}
		mConnection= mUsbManger.openDevice(mUsbDevice);
		if(mConnection == null){
			return UsbStatus.RETURN_ERROR;
		}
		mConnection.claimInterface(mIntf, true);
		mProductDescriptor ="sy usb";
		/*
		byte []arrayOfByte = new byte[256];
		
		int len = mConnection.controlTransfer(128, 6, 0x0301, 0, arrayOfByte,arrayOfByte.length, 2000);
		
		mProductDescriptor ="";
		
		if(len <= 0 ){
			mProductDescriptor +="sy usb";
		}
		else{
			if ((arrayOfByte[1] & 0xff) == 0x03) {
				for (int i = 2; i < (arrayOfByte[0] & 0xFF); i += 2) {
					mProductDescriptor += (char)arrayOfByte[i];
				}
			}
		}
		*/
		return UsbStatus.RETURN_SUCCESS;
	}
	
	public int getVendorId(){
		if(null == mUsbDevice){
			return UsbStatus.RETURN_ERROR;
		}
		return mUsbDevice.getVendorId();
	}
	public int getProductId(){
		if(null == mUsbDevice){
			return UsbStatus.RETURN_ERROR;
		}
		return mUsbDevice.getProductId();
	}
	
	public synchronized int synoDownData( byte[] sendBuf, int sendBufLen)
	{
		int ret=UsbStatus.RETURN_ERROR;
		byte[] receiveBuf = new byte[512];
		int[] receiveBufLen = new int[1];
		try {
			ret = scsi_send_1(sendBuf,sendBufLen);
			if(UsbStatus.RETURN_SUCCESS != ret) {
				return ret;
			}
			//ret = scsi_recv_1(receiveBuf,receiveBufLen);
			//if(UsbStatus.RETURN_SUCCESS != ret) {
			//	return ret;
			//}
		} catch (Exception e) {
			return -1;
		}
		return ret;
	}
	
public static int scsi_send_data(byte[] cmd, byte[] sendBuf, int sendBufLen)
{
	System.out.println("into scsi_send_data function");
	int ret;
	byte []bycbw = new byte[31];
	byte []bycsw = new byte[64];
	int  []receiveBufLen = new int[1];

	bycbw[0] =(byte) 0x55;
	bycbw[1] =(byte) 0x53;
	bycbw[2] =(byte) 0x42;
	bycbw[3] =(byte) 0x43;
	
	bycbw[4] =(byte) 'S';
	bycbw[5] =(byte) 'y';
	bycbw[6] =(byte) 'n';
	bycbw[7] =(byte) '0';

	bycbw[8] =(byte) (sendBufLen & 0xFF);
	bycbw[9] =(byte) ((sendBufLen >> 8) & 0xFF);
	bycbw[10] =(byte) ((sendBufLen >> 16) & 0xFF);
	bycbw[11] =(byte) ((sendBufLen >> 24) & 0xFF);
	
	bycbw[12] =(byte) 0x00;
	bycbw[13] =(byte) 0x00;
	bycbw[14] =(byte) 0x0A;
	
	System.arraycopy(cmd, 0, bycbw, 15, 6);//把命令包拼到cbw
	
	ret = _Write(bycbw , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	if(sendBufLen > 0)
	{
		ret = _Write(sendBuf , sendBufLen);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
	}
	ret = _Read(bycsw , receiveBufLen);
	if(receiveBufLen[0] != 13){
		return UsbStatus.RETURN_ERROR;
	}
	
	return UsbStatus.RETURN_SUCCESS;
}
	
	
public synchronized int scsi_send_1(byte[]sendBuf,int sendBufLen){
		
		int ret;
		byte []bycbw = new byte[31];
		byte []bycsw = new byte[64];
		int  []receiveBufLen = new int[2];

		bycbw[0] =(byte) 0x55;
		bycbw[1] =(byte) 0x53;
		bycbw[2] =(byte) 0x42;
		bycbw[3] =(byte) 0x43;
		
		bycbw[4] =(byte) 'S';
		bycbw[5] =(byte) 'y';
		bycbw[6] =(byte) 'n';
		bycbw[7] =(byte) '0';
	
		bycbw[8] =(byte) (sendBufLen & 0xFF);
		bycbw[9] =(byte) ((sendBufLen >> 8) & 0xFF);
		bycbw[10] =(byte) ((sendBufLen >> 16) & 0xFF);
		bycbw[11] =(byte) ((sendBufLen >> 24) & 0xFF);
		
		bycbw[12] =(byte) 0x00;
		bycbw[13] =(byte) 0x00;
		bycbw[14] =(byte) 0x0A;
		
		bycbw[15] =(byte) 0x86;
		bycbw[16] =(byte) 0x00;
		
		ret = _Write(bycbw , 31);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		ret = _Write(sendBuf , sendBufLen);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		ret = _Read(bycsw , receiveBufLen);
		//这里不校验了，只判断返回
		if(receiveBufLen[0] != 13){
			return UsbStatus.RETURN_ERROR;
		}
		
		
		return UsbStatus.RETURN_SUCCESS;
	}

public synchronized int synoGetData( byte[] DataBuf, int[] getLen)
{
	return scsi_recv_1(DataBuf,getLen);
	/*
	int ret = 0;
	int i;
	byte[] do_CBW = new byte[33];
	byte[] di_CSW = new byte[16];
	int[] diLength = new int[1];
	diLength[0] = 13;
	
	do_CBW[0] = (byte)0x55;
	do_CBW[1] = (byte)0x53;
	do_CBW[2] = (byte)0x42;
	do_CBW[3] = (byte)0x43;
	do_CBW[4] = (byte)'S';
	do_CBW[5] = (byte)'y';
	do_CBW[6] = (byte)'n';
	do_CBW[7] = (byte)'o';
	do_CBW[8] = (byte)(getLen[0] & 0xff);
	do_CBW[9] = (byte)((getLen[0]>>8) & 0xff);
	do_CBW[10]= (byte)((getLen[0]>>16)& 0xff);
	do_CBW[11]= (byte)((getLen[0]>>24)& 0xff);
	do_CBW[12]= (byte)0x80;
	do_CBW[13]= (byte)0x00;
	do_CBW[14]= (byte)0x0a;
	do_CBW[15]= (byte)0x85;
	
	ret = _Write(do_CBW , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-351; 
	}
	ret = _Read(DataBuf , getLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-352; 
	}
	//System.arraycopy(DataBuf, 0, printf1, 0, getLen[0]);
	ret = _Read(di_CSW , diLength);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-353; 
	}
	//System.arraycopy(di_CSW, 0, printf1, 64, diLength[0]);
	if( (di_CSW[3] != 0x53) || (di_CSW[12] != 0x00) )
	{
		return 	-(1000 + diLength[0]);
	}
	for( i=4; i<8; i++)
	{
		if(di_CSW[i] != do_CBW[i])
			return -(355+i);
	}
	return UsbStatus.RETURN_SUCCESS; 
	*/
}
	
public static int scsi_recv_data_1(byte[] recviveBuf, int[] receiveLen)
{
	int ret;
	int res=0;
	System.out.println("into scsi_recv_data11 function");
	byte []bycbw = new byte[31];
	byte []bycsw = new byte[64];
	
	byte[] tmpReceiveBuf=new byte[receiveLen[0]];
	
	bycbw[0] =(byte) 0x55;
	bycbw[1] =(byte) 0x53;
	bycbw[2] =(byte) 0x42;
	bycbw[3] =(byte) 0x43;
	
	bycbw[4] =(byte) 'S';
	bycbw[5] =(byte) 'y';
	bycbw[6] =(byte) 'n';
	bycbw[7] =(byte) 'o';

	bycbw[8] =(byte) (receiveLen[0] & 0xFF);
	bycbw[9] =(byte) ((receiveLen[0] >> 8) & 0xFF);
	bycbw[10] =(byte) ((receiveLen[0] >> 16) & 0xFF);
	bycbw[11] =(byte) ((receiveLen[0] >> 24) & 0xFF);
	
	bycbw[12] =(byte) 0x80;
	bycbw[13] =(byte) 0x00;
	bycbw[14] =(byte) 0x0A;
	
	bycbw[15] =(byte) 0xEF;
	bycbw[16] =(byte) 0x02;
	
	ret = _Write(bycbw , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	//接收响应包
	ret = _Read(tmpReceiveBuf , receiveLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	System.arraycopy(tmpReceiveBuf, 0, recviveBuf, 0, receiveLen[0]);
	//接收csw
	ret = _Read(bycsw , receiveLen);
	if(ret == UsbStatus.RETURN_SUCCESS){
		System.out.println("接收csw1 true tmpReceiveBuf[1]=" + (int)tmpReceiveBuf[1]);
		return 	(int)tmpReceiveBuf[1]; 
	}
	else{
		return -1;
	}
}

public static int scsi_recv_data_2(byte[] recviveBuf, int[] receiveLen)
{
	int ret;
	int res=0;
	System.out.println("into scsi_recv_data12 function");
	byte []bycbw = new byte[31];
	byte []bycsw = new byte[64];
	
	byte[] tmpReceiveBuf=new byte[receiveLen[0]];
	
	bycbw[0] =(byte) 0x55;
	bycbw[1] =(byte) 0x53;
	bycbw[2] =(byte) 0x42;
	bycbw[3] =(byte) 0x43;
	
	bycbw[4] =(byte) 'S';
	bycbw[5] =(byte) 'y';
	bycbw[6] =(byte) 'n';
	bycbw[7] =(byte) 'o';

	bycbw[8] =(byte) (receiveLen[0] & 0xFF);
	bycbw[9] =(byte) ((receiveLen[0] >> 8) & 0xFF);
	bycbw[10] =(byte) ((receiveLen[0] >> 16) & 0xFF);
	bycbw[11] =(byte) ((receiveLen[0] >> 24) & 0xFF);
	
	bycbw[12] =(byte) 0x80;
	bycbw[13] =(byte) 0x00;
	bycbw[14] =(byte) 0x0A;
	
	bycbw[15] =(byte) 0xEF;
	bycbw[16] =(byte) 0x03;

	ret = _Write(bycbw , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	//接收应答数据包
	ret = _Read(tmpReceiveBuf , receiveLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	System.arraycopy(tmpReceiveBuf, 0, recviveBuf, 0, receiveLen[0]);
	//接收csw
	ret = _Read(bycsw , receiveLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-1; 
	}
	return UsbStatus.RETURN_SUCCESS;
}




public synchronized int scsi_recv_1(byte[] receiveBuf,int[] receiveBufLen){
	
	int ret;
	int res=0;
	
	byte []bycbw = new byte[31];
	byte []bycsw = new byte[64];
	int []receiveLen = new int[2];
	//receiveLen[0]=receiveBuf.length +2;
	receiveLen[0] = 64;//receiveBufLen[0];
	//receiveLen[0] = receiveBufLen[0];
	byte[]tmpReceiveBuf=new byte[receiveLen[0]];
	
	bycbw[0] =(byte) 0x55;
	bycbw[1] =(byte) 0x53;
	bycbw[2] =(byte) 0x42;
	bycbw[3] =(byte) 0x43;
	
	bycbw[4] =(byte) 'S';
	bycbw[5] =(byte) 'y';
	bycbw[6] =(byte) 'n';
	bycbw[7] =(byte) 'o';

	bycbw[8] =(byte) (receiveLen[0] & 0xFF);
	bycbw[9] =(byte) ((receiveLen[0] >> 8) & 0xFF);
	bycbw[10] =(byte) ((receiveLen[0] >> 16) & 0xFF);
	bycbw[11] =(byte) ((receiveLen[0] >> 24) & 0xFF);
	
	bycbw[12] =(byte) 0x80;
	bycbw[13] =(byte) 0x00;
	bycbw[14] =(byte) 0x0A;
	
	bycbw[15] =(byte) 0x85;
	bycbw[16] =(byte) 0x00;
	
	ret = _Write(bycbw , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	ret = _Read(tmpReceiveBuf , receiveLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	UsbStatus.RETURN_ERROR; 
	}
	
	if(receiveLen[0]<2){
		return UsbStatus.RETURN_ERROR;
	}
		
	System.arraycopy(tmpReceiveBuf,0,receiveBuf ,0,receiveLen[0] );
	receiveBufLen[0] = receiveLen[0];
	receiveLen[0] = 0x00;
	ret = _Read(bycsw , receiveLen);
	//这里不校验了，只判断返回
	if(receiveLen[0] != 13){
		return UsbStatus.RETURN_ERROR;
	}
	if(res== 0){
		return UsbStatus.RETURN_SUCCESS;
	} else {
		return res;
	}
}

public synchronized int scsi_recv_Image(byte[]receiveBuf,int len, int offset){
	
	int ret;
	int res=0;
	
	byte []bycbw = new byte[31];
	byte []bycsw = new byte[64];
	int []receiveLen = new int[2];
	//receiveLen[0]=receiveBuf.length +2;
	receiveLen[0] = len;//receiveBufLen[0];
	//receiveLen[0] = receiveBufLen[0];
	byte[]tmpReceiveBuf=new byte[receiveLen[0]];
	
	bycbw[0] =(byte) 0x55;
	bycbw[1] =(byte) 0x53;
	bycbw[2] =(byte) 0x42;
	bycbw[3] =(byte) 0x43;
	
	bycbw[4] =(byte) 'S';
	bycbw[5] =(byte) 'y';
	bycbw[6] =(byte) 'n';
	bycbw[7] =(byte) 'o';

	bycbw[8] =(byte) (len & 0xFF);
	bycbw[9] =(byte) ((len >> 8) & 0xFF);
	bycbw[10] =(byte) ((len >> 16) & 0xFF);
	bycbw[11] =(byte) ((len >> 24) & 0xFF);
	
	bycbw[12] =(byte) 0x80;
	bycbw[13] =(byte) 0x00;
	bycbw[14] =(byte) 0x0A;
	
	bycbw[15] =(byte) 0x85;
	bycbw[16] =(byte) 0x00;
	
	ret = _Write(bycbw , 31);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-901; 
	}
	ret = _Read(tmpReceiveBuf , receiveLen);
	if(ret != UsbStatus.RETURN_SUCCESS){
		return 	-902; 
	}
	
	if(receiveLen[0]<2){
		return -903;
	}
		
	System.arraycopy(tmpReceiveBuf,0,receiveBuf ,offset,receiveLen[0] );
	receiveLen[0] = 0x00;
	ret = _Read(bycsw , receiveLen);
	//这里不校验了，只判断返回
	if((bycsw[3]!=0x53) || (bycsw[12] != 0x00))
	{
		return -904;
	}
	if(res== 0){
		return UsbStatus.RETURN_SUCCESS;
	} else {
		return res;
	}
}

/*
	private  int __Read(byte []buffer,int []length ){

		int len = 0;
		try{	
			len= mConnection.bulkTransfer(mPointIn, buffer, buffer.length,30000);
			System.out.println("read 4444 len :"+len);
			length[0] = len;
			String str=Tool.byte2HexStr(buffer, len);
			System.out.print("read:\n");
			System.out.print(str);
			System.out.print("\n");
		}
		catch(Exception e)
		{
			len = 0;
			return UsbStatus.RETURN_ERROR;
		}
		return UsbStatus.RETURN_SUCCESS;
	}	
*/

private static  int _Read(byte []buffer,int []length ){
	
	System.out.println("read length : "+ length[0]);
	if(length[0]>=36864)
	{
		int len = length[0]/4;
		int ret = 0;
		byte[] tempBuf = new byte[len];
		for(int i=0; i<4; i++)
		{
			try{
				ret = mConnection.bulkTransfer(mPointIn, tempBuf, len,300000);
				if(ret < 0)
				{
					return UsbStatus.RETURN_ERROR;
				}
				System.arraycopy(tempBuf, 0, buffer, len*i, len);
			}
			catch(Exception e)
			{
				return UsbStatus.RETURN_ERROR;
			}
		}
		
		return UsbStatus.RETURN_SUCCESS;
	}
	else if( (length[0]>=16384) && (length[0]<36864))
	{
		int len = length[0]/2;
		int ret = 0;
		byte[] tempBuf = new byte[len];
		for(int i=0; i<2; i++)
		{
			try{	
				ret = mConnection.bulkTransfer(mPointIn, tempBuf, len,300000);
				if(ret < 0)
				{
					return UsbStatus.RETURN_ERROR;
				}
				System.arraycopy(tempBuf, 0, buffer, len*i, len);
			}
			catch(Exception e)
			{
				return UsbStatus.RETURN_ERROR;
			}
		}
		
		return UsbStatus.RETURN_SUCCESS;
	}
	else {
		
		int len = 0;
		len = mConnection.bulkTransfer(mPointIn, buffer, buffer.length,300000);
		System.out.println("read 4444 len "+len);
		if(len < 0)
		{
			return UsbStatus.RETURN_ERROR;
		}
		length[0] = len;
		
		String str=Tool.byte2HexStr(buffer, len);
		System.out.print("Read:\n");
		System.out.print(str);
		System.out.print("\n");
		
		return UsbStatus.RETURN_SUCCESS;
	}
}
	private static int _Write(byte []buffer,int length){

		try{
			System.out.println("_Write length = "+length);
			int ret = mConnection.bulkTransfer(mPointOut, buffer, length, 30000);
			if( -1 == ret )
			{
				System.out.print("write data error");
				return UsbStatus.RETURN_ERROR;
			}
			String str=Tool.byte2HexStr(buffer, length);
			System.out.print("write: \n");
			System.out.print(str);
			System.out.print("\n");
		}
		catch(Exception e)
		{
			return UsbStatus.RETURN_ERROR;
		}	
		return UsbStatus.RETURN_SUCCESS;
	}
	
	public String printf_fun(){
		return printf1.toString();
	}
	
public synchronized int scsi_send(TPCCmd cmd,byte[]sendBuf,int sendBufLen){
		
		int ret;
		byte []bycmd = new byte[8];
		byte []bycbw = new byte[31];
		byte []bycsw = new byte[64];
		int  []receiveBufLen = new int[2];
		
		bycmd[0] = (byte)cmd.CLA;
		bycmd[1] = (byte)cmd.INS;
		bycmd[2] = (byte)cmd.P1;
		bycmd[3] = (byte)cmd.P2;
		bycmd[4] = (byte)(cmd.LC & 0xFF);
		bycmd[5] = (byte)(cmd.LC >> 8);
		bycmd[6] = (byte)(cmd.LE & 0xFF);
		bycmd[7] = (byte)(cmd.LE >> 8);
		
		bycbw[0] =(byte) 0x55;
		bycbw[1] =(byte) 0x53;
		bycbw[2] =(byte) 0x42;
		bycbw[3] =(byte) 0x43;
		
		bycbw[4] =(byte) 0x08;
		bycbw[5] =(byte) 0xA0;
		bycbw[6] =(byte) 0x6A;
		bycbw[7] =(byte) 0x87;
	

		bycbw[8] =(byte) (sendBufLen & 0xFF);
		bycbw[9] =(byte) ((sendBufLen >> 8) & 0xFF);
		bycbw[10] =(byte) ((sendBufLen >> 16) & 0xFF);
		bycbw[11] =(byte) ((sendBufLen >> 24) & 0xFF);
		
		bycbw[12] =(byte) 0x00;
		bycbw[13] =(byte) 0x00;
		bycbw[14] =(byte) 0x0A;
		
		bycbw[15] =(byte) 0xEF;
		bycbw[16] =(byte) 0x01;
		
		System.arraycopy(bycmd,0,bycbw ,17, 8);
		
		
		ret = _Write(bycbw , 31);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		ret = _Write(sendBuf , sendBufLen);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		ret = _Read(bycsw , receiveBufLen);
		//这里不校验了，只判断返回
		if(receiveBufLen[0] != 13){
			return UsbStatus.RETURN_ERROR;
		}
		
		
		return UsbStatus.RETURN_SUCCESS;
	}

public synchronized int scsi_recv(TPCCmd cmd,byte[]receiveBuf,int[]receiveBufLen){
		
		int ret;
		int res=0;
		
		byte []bycmd = new byte[8];
		byte []bycbw = new byte[31];
		byte []bycsw = new byte[64];
		int []receiveLen = new int[2];
		receiveLen[0]=receiveBuf.length +2;
		byte[]tmpReceiveBuf=new byte[receiveLen[0]];
		
		
		bycmd[0] = (byte)cmd.CLA;
		bycmd[1] = (byte)cmd.INS;
		bycmd[2] = (byte)cmd.P1;
		bycmd[3] = (byte)cmd.P2;
		bycmd[4] = (byte)(cmd.LC & 0xFF);
		bycmd[5] = (byte)(cmd.LC >> 8);
		bycmd[6] = (byte)(cmd.LE & 0xFF);
		bycmd[7] = (byte)(cmd.LE >> 8);
		
		bycbw[0] =(byte) 0x55;
		bycbw[1] =(byte) 0x53;
		bycbw[2] =(byte) 0x42;
		bycbw[3] =(byte) 0x43;
		
		bycbw[4] =(byte) 0x08;
		bycbw[5] =(byte) 0xA0;
		bycbw[6] =(byte) 0x6A;
		bycbw[7] =(byte) 0x87;
	

		bycbw[8] =(byte) (receiveLen[0] & 0xFF);
		bycbw[9] =(byte) ((receiveLen[0] >> 8) & 0xFF);
		bycbw[10] =(byte) ((receiveLen[0] >> 16) & 0xFF);
		bycbw[11] =(byte) ((receiveLen[0] >> 24) & 0xFF);
		
		bycbw[12] =(byte) 0x80;
		bycbw[13] =(byte) 0x00;
		bycbw[14] =(byte) 0x0A;
		
		bycbw[15] =(byte) 0xEF;
		bycbw[16] =(byte) 0x02;
		
		System.arraycopy(bycmd,0,bycbw ,17, 8);
		
		ret = _Write(bycbw , 31);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		ret = _Read(tmpReceiveBuf , receiveLen);
		if(ret != UsbStatus.RETURN_SUCCESS){
			return 	UsbStatus.RETURN_ERROR; 
		}
		
		if(receiveLen[0]<2){
			return UsbStatus.RETURN_ERROR;
		}
			
		res = tmpReceiveBuf[0]+tmpReceiveBuf[1]*256;
		receiveBufLen[0]=receiveLen[0]-2;
		System.arraycopy(tmpReceiveBuf,2,receiveBuf ,0,receiveBufLen[0] );
		
		receiveLen[0] = 0x00;
		ret = _Read(bycsw , receiveLen);
		//这里不校验了，只判断返回
		if(receiveLen[0] != 13){
			return UsbStatus.RETURN_ERROR;
		}
		if(res== 0){
			return UsbStatus.RETURN_SUCCESS;
		} else {
			return res;
		}

	}

public synchronized int scsi_cmd(TPCCmd cmd,byte[]sendBuf,byte[]receiveBuf,int[]receiveBufLen){
	
	int ret=UsbStatus.RETURN_ERROR;
	TPCCmd cmd2 = new TPCCmd();
	
	cmd2.CLA = 0x00;
	cmd2.INS = 0x00;
	cmd2.P1  =0x00;
	cmd2.P2  =0x00;
	cmd2.LC = 0x00;
	cmd2.LE = 0x00;
	
	try {
		ret = scsi_send(cmd,sendBuf,cmd.LC);
		if(UsbStatus.RETURN_SUCCESS != ret) {
			return ret;
		}
		ret = scsi_recv(cmd2,receiveBuf,receiveBufLen);
		if(UsbStatus.RETURN_SUCCESS != ret) {
			
			return ret;
		}
		
		
	} catch (Exception e) {
		return -1;
	}

	return ret;
}


}
