package com.synochip.sdk.ukey;

import java.io.IOException;

import com.synochip.demo.usb.MainActivity;
import com.synochip.sdk.ukey.Tool;
import com.synochip.sdk.ukey.UsbStatus;

import android.R.integer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;


public class OTG_KEY {
	
	public final static int TYPE_ALG_RSA_1024 = 1;
	public final static int TYPE_ALG_SM2_256 = 2;

	public final static int TYPE_ALG_SHA_1 = 1;
	public final static int TYPE_ALG_SHA_256 = 2;
	public final static int TYPE_ALG_SM3 = 3;
	
	public final static int TYPE_KEY_ENCRYPT = 1;
	public final static int TYPE_KEY_SIGN = 2;
	
	public final static int MAX_PACKAGE_SIZE = 350; //package max size
	public final static int CMD = 0x01;
	public final static byte DATA = 0x02;
	public final static byte ENDDATA = 0x08;
	public final static byte VFY_PWD = 0x13;
	public final static byte RESPONSE = 0x07;
	public final static byte GET_IMAGE = 0x01;
	public final static byte READ_NOTEPAD = 0x19;
	public final static int HEAD_LENGTH = 3;
	public final static byte GEN_CHAR = 0x02;
	public final static byte REG_MODULE = 0x05;
	public final static byte STORE_CHAR = 0x06;
	public final static byte LOAD_CHAR = 0x07;
	public final static byte BURN_CODE = 0x1a;
	public final static byte UP_CHAR = 0x08;
	public final static byte DOWN_CHAR = 0x09;
	public final static byte SEARCH = 0x04;
	public final static int DEV_ADDR = 0xffffffff;
	public final static byte EMPTY = 0x0d;
	public final static byte READ_INDEXTABLE = 0x1f;
	public final static byte UP_IMAGE = 0x0a;
	public final static byte Auto_Cancel = 0x30;
	public final static byte Auto_Enroll = 0x31;
	public final static byte Auto_Identify = 0x32;
	public final static int IMAGE_X = 256;
	public final static int IMAGE_Y = 288;
		
	public final static int DEVICE_SUCCESS = 0x00000000  ;
	public final static int DEVICE_FAILED   =  0x20000001;
	public final static int DEVICE_KEY_REMOVED =   0x20000002;
	public final static int DEVICE_KEY_INVALID   =  0x20000003;
	public final static int DEVICE_INVALID_PARAMETER =   0x20000004;
	public final static int DEVICE_VERIFIEDPIN_FAILED   =  0x20000005;
	public final static int DEVICE_USER_NOT_LOG_IN =   0x20000006;
	
	public final static int DEVICE_BUFFER_TOO_SMALL = 0x20000007 ;
	public final static int DEVICE_CONTAINER_TOOMORE   =  0x20000008;
	public final static int DEVICE_ERR_GETEKEYPARAM =   0x20000009;
	public final static int DEVICE_ERR_PINLOCKED   =  0x20000010;
	public final static int DEVICE_ERR_CREATEFILE =   0x20000011;
	public final static int DEVICE_ERR_EXISTFILE   =  0x20000012;
	public final static int DEVICE_ERR_OPENFILE =   0x20000013;
	
	public final static int DEVICE_ERR_READFILE =   0x20000014;
	public final static int DEVICE_ERR_WRITEFILE   =  0x20000015;
	public final static int DEVICE_ERR_NOFILE =   0x20000016;
	
	public final static int DEVICE_ERR_PARAMETER_NOT_SUPPORT =   0x20000020;
	public final static int DEVICE_ERR_FUNCTION_NOT_SUPPORT   =  0x20000021;
	
	private final static String appName="OTG_DEMO";
	
	public byte[] myprintf= new byte[128];
	
	private UsbBase msyUsbBase ;
		
	private byte[]  mAppHandle = new byte[2];
	private boolean misOpen = false;

	byte LED_1 = 0x01;
	byte PRE_1 = 0x02;
	byte STA_1 = 0x04;
	byte ID_1 = 0x08;
	byte CTL_1 = 0x10;
	byte LEAVE_1 = 0x20;



	byte[] buffer = new byte[MAX_PACKAGE_SIZE];

	public OTG_KEY(UsbManager mManager, UsbDevice mDev) {
		try {
			msyUsbBase = null;
			msyUsbBase = new UsbBase(mManager,mDev);
			
		} catch (Exception e) {
				return;
		}
	}
	
	public int UsbOpen() {
		int ret;
	
		try {
    		ret = msyUsbBase.open();
			
		} catch (Exception e) {
			
			return DEVICE_FAILED;	
		}
    	
    	if( UsbStatus.RETURN_SUCCESS != ret ) {
    		return DEVICE_FAILED;	
    	} 
			
		return DEVICE_SUCCESS;
		
	}

	public int OpenUKey() {
    	int ret;
    	  	
    	ret = UsbOpen();
    	
    	if( UsbStatus.RETURN_SUCCESS != ret ) {
    		return DEVICE_FAILED;	
    	} 
    	
    	TPCCmd cmd = new TPCCmd();
    	
    	byte bAppName[]= appName.getBytes();
    	
    	cmd.CLA = 0x80;
		cmd.INS = 0x26;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = bAppName.length;
		cmd.LE  = 0x00;
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];

		ret = msyUsbBase.scsi_cmd(cmd,bAppName,bRevBuf,length);
		
		if(ret == UsbStatus.RETURN_SUCCESS && length[0] == 10 ) {
			 System.arraycopy(bRevBuf, 8, mAppHandle, 0, mAppHandle.length);
			 misOpen = true;
			 return DEVICE_SUCCESS;
		} else {
			java.util.Arrays.fill(mAppHandle, (byte) 0);
			misOpen = false;
			return ret;
		}
	}

	void close()
	{
		misOpen = false;
		try {
			msyUsbBase.close();
		} catch (Exception e) {
			return;
		}
	}	
	public int CloseCard(int hKey)
    {
		close();		
    	misOpen = false;
    	return DEVICE_SUCCESS;
    }
	
	public int UserLogin(String userPin)
	{
		
		if(!misOpen)
			return DEVICE_FAILED;
		
		if(userPin.length() > 16) {
			return -1;
		}
		
		byte[] bRandom = genRandom((int)8);
		if(bRandom==null){
			return -1;
		}
		
    	TPCCmd cmd = new TPCCmd();
		cmd.CLA = 0x80;
		cmd.INS = 0x18;
		cmd.P1  = 0x00;
		cmd.P2  = 0x01;
		cmd.LC  = (int)(8+2);
		cmd.LE  = 0x00;

		byte[] bSendBuf = new byte[cmd.LC];
		byte[] bUserPin = userPin.getBytes();
		byte[] bByteKey = new byte[8];
		System.arraycopy(bUserPin, 0, bByteKey, 0, bUserPin.length);
						
		byte[] bAuthData = DESUtil.encryptByte(bRandom,bByteKey);
		
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bAuthData,(int)0,bSendBuf,(int)2,bAuthData.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;

		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return 0;
		} else if ( ret == 0x25 ) {
			 return UsbStatus.RETURN_PINLOCK;
		}else if ((ret&0xff) == 0x26 ) {
			return ((ret>>8) & 0xff)+1;
		}
		else{
			 return -1;
		}
	}
    
	public int AdminLogin(String userPin)
	{
		
		if(!misOpen)
			return DEVICE_FAILED;
		
		if(userPin.length() > 16) {
			return -1;
		}
		
		byte[] bRandom = genRandom((int)8);
		if(bRandom==null){
			return -1;
		}
		
    	TPCCmd cmd = new TPCCmd();
		cmd.CLA = 0x80;
		cmd.INS = 0x18;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = (int)(8+2);
		cmd.LE  = 0x00;

		byte[] bSendBuf = new byte[cmd.LC];
		byte[] bUserPin = userPin.getBytes();
		byte[] bByteKey = new byte[8];
		System.arraycopy(bUserPin, 0, bByteKey, 0, bUserPin.length);
						
		byte[] bAuthData = DESUtil.encryptByte(bRandom,bByteKey);
		
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bAuthData,(int)0,bSendBuf,(int)2,bAuthData.length);
			
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;

		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return 0;
		} else if ( ret == 0x25 ) {
			 return UsbStatus.RETURN_PINLOCK;
		}else if ((ret&0xff) == 0x26 ) {
			return ((ret>>8) & 0xff)+1;
		}
		else{
			 return -1;
		}
	}
	
	public int Delock(String adminPin, String userPin)
	{
		
		if(!misOpen)
			return DEVICE_FAILED;
		
		if(userPin.length() > 16) {
			return -1;
		}
		byte[] adminpin = adminPin.getBytes();
		byte[] userpin = userPin.getBytes();
		
    	TPCCmd cmd = new TPCCmd();
		cmd.CLA = 0x84;
		cmd.INS = 0x1A;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x12;
		cmd.LE  = 0x00;

		byte[] bSendBuf = new byte[cmd.LC];
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		
		byte[] bPlain = new byte[16];
		System.arraycopy(userpin, 0,bPlain, 0, userpin.length);
		bPlain[8]= (byte)0x80;
		
		byte[] bKey = new byte[8];
		System.arraycopy(adminpin, 0, bKey, 0, adminpin.length);				
		
		byte bCiph[] = DESUtil.encryptByte(bPlain, bKey);
		
		System.arraycopy(bCiph, 0, bSendBuf, 2, bCiph.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd, bSendBuf, bRevBuf, length);		
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return 0;
		} else {
			 return -1;
		}		
	}
	
	public boolean logout()
	{
		
		if(!misOpen){
			return false;
		}
		
    	TPCCmd cmd = new TPCCmd();
		cmd.CLA = 0x80;
		cmd.INS = 0x1C;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x02;
		cmd.LE  = 0x00;

		byte[] bSendBuf = new byte[cmd.LC];		
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;

		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return true;
		} else {
			 return false;
		}
	}
	
	public boolean CleanFlag()
	{
		if(!misOpen){
			return false;
		}
		
    	TPCCmd cmd = new TPCCmd();
    	cmd.CLA = 0x80;
		cmd.INS = 0x07;
		cmd.P1  = 0x01;
		cmd.P2  = 0x00;
		cmd.LC  = 0x02;
		cmd.LE  = 0x00;


		byte[] bSendBuf = new byte[cmd.LC];		
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;

		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return true;
		} else {
			 return false;
		}
	}
	
	public boolean modifyUserPin(String oldUserPin, String newUserPin)
	{
		if(!misOpen){
			return false;
		}		
		
		if((oldUserPin.length() > 16)||(newUserPin.length() > 16)){
			return false;
		}
					
		byte[] bOldPin = oldUserPin.getBytes();
		byte[] bNewPin = newUserPin.getBytes();
		
    	TPCCmd cmd = new TPCCmd();
    	
		cmd.CLA = 0x80;
		cmd.INS = 0x16;
		cmd.P1  = 0x00;
		cmd.P2  = 0x01;
		cmd.LC  = 0x12;
		cmd.LE  = 0x00;
		
		byte[] bSendBuf = new byte[cmd.LC];
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		
		byte[] bPlain = new byte[16];
		System.arraycopy(bNewPin, 0,bPlain, 0, bNewPin.length);
		bPlain[8]= (byte)0x80;
		
		byte[] bKey = new byte[8];
		System.arraycopy(bOldPin, 0, bKey, 0, bOldPin.length);				
		
		byte bCiph[] = DESUtil.encryptByte(bPlain, bKey);
		
		System.arraycopy(bCiph, 0, bSendBuf, 2, bCiph.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd, bSendBuf, bRevBuf, length);		
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return true;
		} else {
			 return false;
		}		
	}
		
	public boolean AdminModifyUserPin(String oldUserPin, String newUserPin)
	{
		if(!misOpen){
			return false;
		}		
		
		if((oldUserPin.length() > 16)||(newUserPin.length() > 16)){
			return false;
		}
					
		byte[] bOldPin = oldUserPin.getBytes();
		byte[] bNewPin = newUserPin.getBytes();
		
    	TPCCmd cmd = new TPCCmd();
    	
		cmd.CLA = 0x80;
		cmd.INS = 0x16;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x12;
		cmd.LE  = 0x00;
		
		byte[] bSendBuf = new byte[cmd.LC];
		System.arraycopy(mAppHandle, 0,bSendBuf, 0, mAppHandle.length);
		
		byte[] bPlain = new byte[16];
		System.arraycopy(bNewPin, 0,bPlain, 0, bNewPin.length);
		bPlain[8]= (byte)0x80;
		
		byte[] bKey = new byte[8];
		System.arraycopy(bOldPin, 0, bKey, 0, bOldPin.length);				
		
		byte bCiph[] = DESUtil.encryptByte(bPlain, bKey);
		
		System.arraycopy(bCiph, 0, bSendBuf, 2, bCiph.length);
		
		byte[] bRevBuf = new byte[256];
		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd, bSendBuf, bRevBuf, length);		
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return true;
		} else {
			 return false;
		}		
	}
	
	public byte[] genRandom(int randomLen)
    {		
		if(misOpen==false){
    		return null;
    	}
    	
    	if((randomLen<8)||(randomLen > 256)) {
    		return null;
    	}
    	
    	TPCCmd cmd = new TPCCmd();
    	
		cmd.CLA = 0x80;
		cmd.INS = 0x50;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC =  0x02;
		cmd.LE =  (int)randomLen;

		byte[] bSendBuf =new byte[2];
    	byte[] bRndBuf = new byte[cmd.LE];
		
    	int[] length = new int[1];
    	
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRndBuf,length);		
		if(ret == UsbStatus.RETURN_SUCCESS ) {			
			 return bRndBuf;
		} else {
			 return null;
		}
    }

	public int generateKeyPair(String containerName, int algType)
    {
		byte[] bhContainer = new byte[2];
		
		if(!misOpen) {
			return -1;
		}
		
		if(containerName.length()>64) {
			return -1;
		}
		
		byte[] bContainerName = containerName.getBytes();
		
		TPCCmd cmd = new TPCCmd();
		
		//Proc 1  Create container
		cmd.CLA = 0x80;
		cmd.INS = 0x40;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
	   
		byte[] bSendBuf = new byte[cmd.LC];
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);
		
		byte[] bRevBuf = new byte[1024];
		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		//if(ret != syUsbStatus.RETURN_SUCCESS ) {
		//	 return -1;
		//}
		
		//Proc 2  Open container
		cmd.CLA = 0x80;
		cmd.INS = 0x42;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
		
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);

		length[0] = bRevBuf.length;
		
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return -1;
		}
		else {
			System.arraycopy(bRevBuf, 0, bhContainer, 0, bhContainer.length);
		}
		
		//Proc 3  Generate key pair
		if(algType == TYPE_ALG_RSA_1024) {
			cmd.CLA = 0x80;
			cmd.INS = 0x54;
			cmd.P1  = 0x01;
			cmd.P2  = 0x00;
			cmd.LC  = 0x06;
			cmd.LE  = 0x84;
			
			System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
			System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
			
			byte[] bKeyBitLen = Tool.hand2Hex(1024);
			System.arraycopy(bKeyBitLen, 0, bSendBuf, 4, bKeyBitLen.length);
					
			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return -1;
			}
			
		} else if(algType == TYPE_ALG_SM2_256) {
			cmd.CLA = 0x80;
			cmd.INS = 0x70;
			cmd.P1  = 0x01;
			cmd.P2  = 0x00;
			cmd.LC  = 0x08;
			cmd.LE  = 0x40;
			
			System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
			System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
			
			byte[] bKeyBitLen = Tool.hand2Hex(256);
			System.arraycopy(bKeyBitLen, 0, bSendBuf, 4, bKeyBitLen.length);
			bSendBuf[6]=0;
			bSendBuf[7]=0;
			
			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return -1;
			}

		} else {
			return -1;
		}
		
		return 0;
		
    }

	public byte[] signData(String containerName , int keyType, int hashType, byte[] inData ,byte[] getSignSendData,int[] getSignSendLen) {
		byte[] bhContainer = new byte[2];
		
		if(!misOpen) {
			return null;
		}
		
		if(containerName.length()>64) {
			return null;
		}
		
		byte[] bContainerName = containerName.getBytes();
		
		TPCCmd cmd = new TPCCmd();
		byte[] bSendBuf = new byte[256];
		byte[] bRevBuf = new byte[256];
				
		//Proc 1  Open container
		cmd.CLA = 0x80;
		cmd.INS = 0x42;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
				
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);

		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return null;
		}
		else {
			System.arraycopy(bRevBuf, 0, bhContainer, 0, bhContainer.length);
		}
		
		//Proc 2   Hash operation
		//Proc 2.1 Initial
		if(hashType==TYPE_ALG_SHA_1){
			cmd.P2  = 0x02;
		} else if(hashType==TYPE_ALG_SM3){
			cmd.P2  = 0x01;
		} else {
			return null;
		}
			
		cmd.CLA = 0x80;
		cmd.INS = (byte)0xB4;
		cmd.P1  = 0x00;
		cmd.LC  = 0x02;
		cmd.LE  = 0x00;
		
		length[0] = bRevBuf.length;
		
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return null;
		}
		/*
		else {
			System.arraycopy(bRevBuf, 0, bhContainer, 0, bhContainer.length);
		}
		*/
		//Proc 2.1 Update
		int iBlkNum = inData.length/256;
		int iRemLen = inData.length%256;
		int iOffset = 0;

		cmd.CLA = 0x80;
		cmd.INS = (byte)0xB8;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x100;
		cmd.LE  = 0x00;
				
		for(int i=0;i<iBlkNum;i++)
		{
			length[0] = bRevBuf.length;
			System.arraycopy(inData, iOffset, bSendBuf, 0, 256);

			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return null;
			}
			
			iOffset += 256;
		}
		
		if(iRemLen>0)
		{
			cmd.LC  = iRemLen;
			
			length[0] = bRevBuf.length;
			System.arraycopy(inData, iOffset, bSendBuf, 0, iRemLen);

			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return null;
			}			
		}
		
		//Proc 2.3 Final
		cmd.CLA = 0x80;
		cmd.INS = (byte)0xBA;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x02;
		cmd.LE  = 0x00;
		
		bSendBuf[0] = 0;
		bSendBuf[1] = 0;
		
		length[0] = bRevBuf.length;
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return null;
		}
				
		byte[] bDegistResult = new byte[length[0] - 4];
		System.arraycopy(bRevBuf, 4, bDegistResult, 0, bDegistResult.length);			

		//Proc 4   Get container type
		cmd.CLA = 0x80;
		cmd.INS = (byte)0xE6;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x02;
		cmd.LE  = 0x00;
	
		System.arraycopy(bhContainer, 0, bSendBuf, 0, bhContainer.length);
		length[0] = bRevBuf.length;

		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return null;
		}
		
		int iAlgType = (int)bRevBuf[0];
		
		//Proc 5  Generate Signature			
		if(iAlgType == TYPE_ALG_RSA_1024) {
			cmd.CLA = 0x80;
			cmd.INS = 0x58;
			cmd.P1  = 0x00;
			cmd.P2  = 0x00;
			cmd.LC  = bDegistResult.length + 4;
			cmd.LE  = 0x104;

			System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
			System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
			System.arraycopy(bDegistResult, 0, bSendBuf, 4, bDegistResult.length);
			
			length[0] = bRevBuf.length;
			getSignSendLen[0] = bDegistResult.length+4;
			System.arraycopy(bSendBuf, 4, getSignSendData, 0, bDegistResult.length+4);
			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return null;
			} 
		} else if(iAlgType == TYPE_ALG_SM2_256) {
			
			cmd.CLA = 0x80;
			cmd.INS = 0x74;
			cmd.P1  = 0x00;
			cmd.P2  = 0x00;
			cmd.LC  = bDegistResult.length + 4;
			cmd.LE  = 0x104;

			System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
			System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
			System.arraycopy(bDegistResult, 0, bSendBuf, 4, bDegistResult.length);
			
			length[0] = bRevBuf.length;
			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				 return null;
			} 
	
		}
		
		byte[] bSignData = new byte[length[0]-4];
		System.arraycopy(bRevBuf, 4, bSignData, 0, bSignData.length);
		
		return bSignData;
		
	}
	
	public boolean verifySignedData( byte[] cert , byte[] inData, 
			byte[] signedValue, int hashType ) {

		return true;
		
	}	
	
	public boolean importCertificate(String containerName, 
			int keyType, byte[] cert){
		
		byte[] bhContainer = new byte[2];
		
		if(!misOpen) {
			return false;
		}
		
		if(containerName.length()>64) {
			return false;
		}
		
		if(cert.length>1024){
			return false;
		}
		
		byte[] bContainerName = containerName.getBytes();
		
		TPCCmd cmd = new TPCCmd();
		byte[] bSendBuf = new byte[1033];
		byte[] bRevBuf = new byte[256];
				
		//Proc 1  Open container
		cmd.CLA = 0x80;
		cmd.INS = 0x42;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
				
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);

		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return false;
		}
		else {
			System.arraycopy(bRevBuf, 0, bhContainer, 0, bhContainer.length);
		}		
				
		//Proc 2  Import certification
		cmd.CLA = 0x80;
		cmd.INS = 0x4C;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 9 + cert.length;
		cmd.LE  = 0x00;
		
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
		
		if(keyType == 0x01){
			bSendBuf[4] = 0x00;
		} else {
			bSendBuf[4] = 0x01;
		}		
				
		byte[] bCertLen=Tool.int2Hex(cert.length);
		System.arraycopy(bCertLen, 0, bSendBuf, 5, bCertLen.length);		
		
		System.arraycopy(cert, 0, bSendBuf, 9, cert.length);
				
		length[0] = bRevBuf.length;		
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			 return true;
		} else {
			 return false;
		}				
	}
	
	public byte[] exportCertificate(String containerName, int keyType ){
		
		byte[] bhContainer = new byte[2];
		
		if(!misOpen) {
			return null;
		}
		
		if((containerName.length()>64)||(containerName.length()<1)) {
			return null;
		}
		
		byte[] bContainerName = containerName.getBytes();
		
		TPCCmd cmd = new TPCCmd();
		byte[] bSendBuf = new byte[256];
		byte[] bRevBuf = new byte[1033];
				
		//Proc 1  Open container
		cmd.CLA = 0x80;
		cmd.INS = 0x42;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
				
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);

		int[] length = new int[1];
		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return null;
		}
		else {
			System.arraycopy(bRevBuf, 0, bhContainer, 0, bhContainer.length);
		}		
				
		//Proc 2  Export certification
		cmd.CLA = 0x80;
		cmd.INS = 0x4E;
		cmd.P2  = 0x00;
		cmd.LC  = 0x04;
		cmd.LE  = bRevBuf.length;
		
		if(keyType == 0x01){
			cmd.P1 = 0x00;
		} else {
			cmd.P1 = 0x01;
		}		
		
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bhContainer, 0, bSendBuf, 2, bhContainer.length);
	
		length[0] = bRevBuf.length;		
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS ) {
			if(length[0] < 2){
				return null;
			}
			
			byte[] cert = new byte[length[0] - 2];			
			System.arraycopy(bRevBuf, 2, cert, 0, cert.length);
			
			return cert;
					
		} else {
			 return null;
		}				
	}	
	
	public boolean openContainerState(String containerName)
	{
		
		if(containerName.length() > 64)
		{
			return false;
		}
		TPCCmd cmd = new TPCCmd();
		byte[] bContainerName = containerName.getBytes();
		byte[] bSendBuf = new byte[128];
		byte[] bRevBuf = new byte[128];
		int[] length = new int[1];
		
		cmd.CLA = 0x80;
		cmd.INS = 0x42;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 2 + bContainerName.length;
		cmd.LE  = 0x02;
				
		System.arraycopy(mAppHandle, 0, bSendBuf, 0, mAppHandle.length);
		System.arraycopy(bContainerName, 0, bSendBuf, 2, bContainerName.length);

		length[0] = bRevBuf.length;
		
		int ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 return false;
		}
		else {
			return true;
		}		
	}
	
	public boolean initDevice(String superPin, String adminPin, String userPin, int retryNum)
	{
		int ret = UsbStatus.RETURN_ERROR;	
		boolean blAppExist=false;
		
		TPCCmd cmd = new TPCCmd();
		
		byte[] bSendBuf = new byte[128];
		byte[] bRevBuf = new byte[128];
						
		ret = UsbOpen();    	
    	if( UsbStatus.RETURN_SUCCESS != ret ) {
    		return false;	
    	} 
    	
		misOpen=true;
		
		//Proc 1  Device Authenticate
		byte[] bRandom = genRandom((int)8);
		if(bRandom==null){
			return false;
		}
				
		cmd.CLA = 0x80;
		cmd.INS = 0x10;
		cmd.P1  = 0x00;
		cmd.P2  = 0x02;
		cmd.LC = 0x08;
		cmd.LE = 0x00 ;
		
		byte[] bSuperPin = new byte[24];				
		byte[] bTmpArray=Tool.hexStringToBytes(superPin);
		
		if(bTmpArray.length <= 24){
			//	
			System.arraycopy(bTmpArray,(int)0,bSuperPin,(int)0,bTmpArray.length);
		}
		else{
			//
			System.arraycopy(bTmpArray,(int)0,bSuperPin,(int)0,(int)24);			
		}
				
		byte[] bAuthData = DESUtil.triEncryptByte(bRandom,bSuperPin);
		System.arraycopy(bAuthData,(int)0,bSendBuf,(int)0,(int)8);
		
		int[] length = new int[1];
		length[0] = bRevBuf.length;
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS  ) {
			close();
			return false;
		}
		
		//Proc 2  Check exist of application
        byte bAppName[]= appName.getBytes();
		
		cmd.CLA = 0x80;
		cmd.INS = 0x26;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC = bAppName.length;
		cmd.LE = 0x0A ;
				
		System.arraycopy(bAppName,0,bSendBuf ,0,bAppName.length);
		
		length[0] = bRevBuf.length;
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		
		if(ret == UsbStatus.RETURN_SUCCESS && length[0] == 10 ) {			
			blAppExist = true;
		}
		
		//Proc 3  Delete application existed
		if(blAppExist && true) {
			
			cmd.CLA = 0x80;
			cmd.INS = 0x24;
			cmd.P1  = 0x00;
			cmd.P2  = 0x00;
			cmd.LC  = bAppName.length;
			cmd.LE  = 0x0A ;
			
			length[0] = bRevBuf.length;
			ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
			if(ret != UsbStatus.RETURN_SUCCESS ) {
				close();
				return false;
			} 
		}

		//Proc 4  Create application
		cmd.CLA = 0x80;
		cmd.INS = 0x20;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC  = 0x60;
		cmd.LE  = 0x0A;
		
		java.util.Arrays.fill(bSendBuf, (byte) 0x00);
		
		System.arraycopy(bAppName, 0, bSendBuf, 0, bAppName.length);
		
		byte[] bAdminPin=Tool.hexStringToBytes(adminPin);
		System.arraycopy(bAdminPin, 0, bSendBuf, 0x30, bAdminPin.length);
		
		byte[] bRetryNum = Tool.int2Hex(retryNum);
		System.arraycopy(bRetryNum, 0, bSendBuf, 0x40, bRetryNum.length);
		
		byte[] bUserPin=Tool.hexStringToBytes(userPin);
		System.arraycopy(bUserPin, 0, bSendBuf, 0x44, bUserPin.length);
		System.arraycopy(bRetryNum, 0, bSendBuf, 0x54, bRetryNum.length);
		
		bSendBuf[0x58]=(byte) 0xFF;
		//bSendBuf[0x59]=0x00;
	    //bSendBuf[0x5A]=0x00;
	    //bSendBuf[0x5B]=0x00;
	    //bSendBuf[0x5C]=0x00;
	    bSendBuf[0x5D]=0x10;
	    bSendBuf[0x5E]=0x10;
	    //bSendBuf[0x5F]=0x00;
						
		length[0] = bRevBuf.length;
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret != UsbStatus.RETURN_SUCCESS ) {
			 close();
			return false;
		}
		
		//Proc 4  Open application
		cmd.CLA = 0x80;
		cmd.INS = 0x26;
		cmd.P1  = 0x00;
		cmd.P2  = 0x00;
		cmd.LC = bAppName.length;
		cmd.LE = 0x0A;
	   
		System.arraycopy(bAppName,0,bSendBuf ,0,bAppName.length);
				
		length[0] = bRevBuf.length;
		
		ret = msyUsbBase.scsi_cmd(cmd,bSendBuf,bRevBuf,length);
		if(ret == UsbStatus.RETURN_SUCCESS && length[0] == 10 ) {
			System.arraycopy(bRevBuf, 8, mAppHandle, 0, mAppHandle.length);
		} 
		else {
			close();
			java.util.Arrays.fill(mAppHandle, (byte) 0);	
			return false;
		}
		
		return true;
		
	}
	
	//add by syno finger usb
	public int fillPackage( byte[] pData, int packageType, int length, byte[] pContent )
	{
		int totalLen = 0;
		
		if( pData.length <= 0 ){
			return -801;
		}
		if( length <= 0 || length > MAX_PACKAGE_SIZE){
			return -802;
		}
		if( (packageType != CMD) && (packageType != DATA) && (packageType != ENDDATA)){
			return -803;
		}
		length+=2;
		pData[0] = (byte)packageType;
		pData[1] = (byte)((length>>8)&0xff);
		pData[2] = (byte)(length&0xff);
		
		System.arraycopy(pContent, 0, pData, 3, length);
		
		totalLen = length +3;
		return totalLen;
	}
	
	
	public int fillPackage_new( byte[] pData, int packageType, int length, byte[] pContent )
	{
		int totalLen = 0;
		
		if( pData.length <= 0 ){
			return -801;
		}
		if( length <= 0 || length > MAX_PACKAGE_SIZE){
			return -802;
		}
		if( (packageType != CMD) && (packageType != DATA) && (packageType != ENDDATA)){
			return -803;
		}
		length+=2;
		pData[0] = (byte)packageType;
		pData[1] = (byte)((length>>8)&0xff);
		pData[2] = (byte)(length&0xff);
		
		System.arraycopy(pContent, 0, pData, 3, length-2);
		
		totalLen = length +3;
		return totalLen;
	}
	
	public int DeCodeUSB(byte[] source, int sourceLen, byte[] destination, int[] destinationLength)
	{
		int i;
		/*
		if( (source[0] != 0xef) || (source[1] != 0x01) ){
			return -501;
		}
		*/
		int nLen = (source[7]<<8)+source[8]+1;
		for(i=0;i<nLen;i++)
		{
			destination[i] = source[6+i];
		}
		destinationLength[0] = nLen;
		return 0;
	}
	public int EnCode(int addr, byte[] source, int sourceLen, byte[] destination, int[] destinationLength)
	{
		int i = 0,chkSum = 0;
		int ValH,ValL;
		if(sourceLen > MAX_PACKAGE_SIZE){
			return -1;
		}
		destination[0] = (byte)0xef;
		destination[1] = (byte)0x01;
		destination[2] = (byte)((addr>>24)&0xff);
		destination[3] = (byte)((addr>>16)&0xff);
		destination[4] = (byte)((addr>>8)&0xff);
		destination[5] = (byte)(addr&0xff);
		for( i = 0; i < sourceLen-2;i++)
		{
			chkSum += source[i];
			destination[6+i] = source[i];
		}
		ValL = (int)(chkSum&0xff);
		ValH = (int)((chkSum>>8)&0xff);
		destination[6+sourceLen-2] = (byte)ValH;
		destination[6+sourceLen-1] = (byte)ValL;
		destinationLength[0] = sourceLen+6;
		return 0;
	}
	
	public int VerifyResponsePackage(byte packageTypt, byte[] data)
	{
		if( data[0] != packageTypt){
			return -201;
		}
		int length = GetPackageLength(data);
		if( packageTypt == RESPONSE)
		{
			return data[3];
		}
		return 0;
	}
	
	public int GetPackageLength(byte[] data)
	{
		int length = 0;
		length = ((int)0xff&data[1])*256 +(int)0xff&data[2]+1+2;
		return length;
	}
	
	public int SendPackageUDisk(int addr, byte[] data)
	{
		int iLength;
		int ret;
		int[] iEncodedLength = new int[1];
		byte[] encodeBuf = new byte[MAX_PACKAGE_SIZE+20];
		iLength = GetPackageLength(data);
		if(iLength > MAX_PACKAGE_SIZE){
			return -4;
		}
		if( 0 != EnCode(addr, data, iLength, encodeBuf, iEncodedLength)){
			return -5;
		}
		if( iEncodedLength[0] > MAX_PACKAGE_SIZE){
			return -6;
		}
		if((ret = msyUsbBase.synoDownData(encodeBuf, iEncodedLength[0])) != 0){
			return ret;
		}
		return 0;
	}
	
	public int SendPackageUDiskDownChar(int addr, byte[] data)
	{
		int iLength;
		int ret;
		int[] iEncodedLength = new int[1];
		byte[] encodeBuf = new byte[MAX_PACKAGE_SIZE+20];
		iLength = GetPackageLength(data);
		if(iLength > MAX_PACKAGE_SIZE){
			return -4;
		}
		//if( 0 != EnCode(addr, data, iLength, encodeBuf, iEncodedLength)){
		//	return -5;
		//}
		//if( iEncodedLength[0] > MAX_PACKAGE_SIZE){
		//	return -6;
		//}
		if((ret = msyUsbBase.synoDownData(data, 512)) != 0){
			return ret;
		}
		return 0;
	}
	public int GetPackageUDisk(byte[] data )
	{
		int[] len = new int[1];
		len[0] = 64;
		int ret;
		byte[] recvBuf = new byte[1024];
		int[] desLen = new int[1];
		if( (ret = msyUsbBase.synoGetData(recvBuf, len)) != 0){
			return ret;
		}
		if( (ret = DeCodeUSB(recvBuf, len[0], data, desLen)) != 0){
			return ret;
		}
		return 0;
	}
	
	
	public int GetPackageUDiskSearch(byte[] data )
	{
		int[] len = new int[1];
		len[0] = 64;
		int ret;
		byte[] recvBuf = new byte[1024];
		int[] desLen = new int[1];
		if( (ret = msyUsbBase.synoGetData(recvBuf, len)) != 0){
			return ret;
		}
		
		if( (ret = DeCodeUSB(recvBuf, len[0], data, desLen)) != 0){
			return ret;
		}
		return 0;
	}
	
	
	public int GetPackageCharUDisk( byte[] data )
	{
		int[] len = new int[1];
		len[0] = 512;
		int ret;
		byte[] recvBuf = new byte[1024];
		int[] desLen = new int[1];
		if( (ret = msyUsbBase.scsi_recv_Image(recvBuf, len[0], 0)) != 0){
			return ret;
		}
		System.out.println( "upChar msyUsbBase = "+bytesToHexString(recvBuf));
		//if( (ret = DeCodeUSB(recvBuf, len[0], data, desLen)) != 0){
		//	return ret;
		//}
		System.arraycopy(recvBuf, 0, data, 0, 512);
		return 0;
	}
	
	
	public int GetPackageImageUDisk( byte[] data, int Imagelen )
	{
		int iTmpLen1 = 36*1024;
		//iTmpLen = Imagelen/9;
		int ret;
		int i;
		int j;
		long[] ret_time = new long[3];
		for( i = 0; i<2; i++)
		{
			j=i*iTmpLen1;
			if( (ret = msyUsbBase.scsi_recv_Image(data,iTmpLen1,j)) != 0){
				return ret;
			}
		}
		//return msyUsbBase.scsi_recv_Image(data, len, iTmpLen,iTmpLen);
		return 0;
	}
	
	public int PSVfyPwd(int nAddr, byte[] passWord)
	{
		int num;
		int result;
		int ret;
		byte[] content = new byte[10];
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		content[0] = VFY_PWD;
		content[1] = passWord[0];
		content[2] = passWord[1];
		content[3] = passWord[2];
		content[4] = passWord[3];
		
		if( (num = fillPackage( sendData, CMD, 5, content )) <= 0 )
		{
			return num;
		}
		if((ret = SendPackageUDisk(nAddr, sendData)) != 0){
			return ret;
		}
		//myprintf = msyUsbBase.printf_fun().getBytes();
		if((ret = GetPackageUDisk(getData)) != 0){
			return ret;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSGetImage(int addr)
	{
		int num;
		byte[] cmd = new byte[10];
		int ret;
		int result;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = GET_IMAGE;
		num = fillPackage(sendData, CMD, 1, cmd);
		if((ret = SendPackageUDisk(addr, sendData)) != 0){
			return -1;
		}
		if(0 != GetPackageUDisk(getData)){
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSClearFlag(int addr)
	{
		int num;
		byte[] cmd = new byte[10];
		int ret;
		int result;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = BURN_CODE;
		cmd[1] = (byte)0;
		num = fillPackage(sendData, CMD, 2, cmd);
		if((ret = SendPackageUDisk(addr, sendData)) != 0){
			return -1;
		}
		if(0 != GetPackageUDisk(getData)){
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSReadInfo(int addr, int page, byte[] userContent)
	{
		int num;
		int result;
		int i;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[1024];
		byte[] content = new byte[2];
		
		content[0] = READ_NOTEPAD;
		content[1] = (byte)page;
		num = fillPackage(sendData, CMD, 2, content);
		if((ret = SendPackageUDisk(addr, sendData)) != 0){
			return -1;
		}
		if(0 != GetPackageUDisk(getData)){
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		if(result != 0)
		{
			return result;
		}
		for(i=0;i<32;i++)
		{
			userContent[i] = getData[HEAD_LENGTH+1+i];
		}
		return result;
	}
	
	public int PSGenChar(int addr, int bufferID)
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = GEN_CHAR;
		cmd[1] = (byte)bufferID;
		num = fillPackage(sendData, CMD, 2, cmd);
		if( num == -1)
		{
			return -3;
		}
		if((ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return ret;
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSRegModule(int addr)
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = REG_MODULE;
		num = fillPackage(sendData, CMD, 1, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -1;
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSStoreChar(int addr, int bufferID, int pageID)
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		if((bufferID<1)||(bufferID>3)||(pageID<0))
		{
			return -4;
		}
		
		cmd[0] = STORE_CHAR;
		cmd[1] = (byte)bufferID;
		cmd[2] = (byte)((pageID>>8)&0xff);
		cmd[3] = (byte)(pageID & 0xff);
		num = fillPackage(sendData, CMD, 4, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -1;
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSSearch(int addr, int bufferID, int startPage, int pageNum, int[] mbAddr )
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		if((bufferID<1)||(bufferID>3)||(startPage<0)||(pageNum<0))
		{
			return -4;
		}
		
		cmd[0] = SEARCH;
		cmd[1] = (byte)bufferID;
		cmd[2] = (byte)((startPage>>8)&0xff);
		cmd[3] = (byte)(startPage & 0xff);
		cmd[4] = (byte)((pageNum>>8)&0xff);
		cmd[5] = (byte)(pageNum & 0xff);
		num = fillPackage(sendData, CMD, 6, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -1;
		}
		if( 0 != GetPackageUDiskSearch(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		mbAddr[0] = (int)getData[HEAD_LENGTH+1]<<8;
		mbAddr[0] |= getData[HEAD_LENGTH+2];
		return result;
	}
	
	public int PSOpen()
	{
		byte[] pwd = new byte[5];
		int i;
		int ret;
		for(i=0; i<5; i++)
		{
			pwd[i] = (byte)0x00;
		}
		if( DEVICE_SUCCESS != UsbOpen()){
			return -101;
		}
		
		return 0;
	}
	
	public int PSEmpty( int addr )
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = EMPTY;
		num = fillPackage(sendData, CMD, 1, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -1;
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}
	
	public int PSword(){
		byte[] pwd = new byte[5];
		int ret;
		/*
		for(i=0; i<5; i++)
		{
			pwd[i] = (byte)0x00;
		}
		*/
		if( (ret = PSVfyPwd(DEV_ADDR,pwd)) != DEVICE_SUCCESS ){
			return ret;
		}
		return 0;
	}
	
	public int PSReadIndexTable( int addr, int nPage, byte[] UserContent)
	{
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = READ_INDEXTABLE;
		cmd[1] = (byte)nPage;
		
		num = fillPackage(sendData, CMD, 2, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -1;
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		result = VerifyResponsePackage(RESPONSE, getData);
		if( result != 0 )
			return result;
		System.arraycopy(getData, 4, UserContent, 0, 32);
		return result;
	}
	
	public int PSUpImage( int addr, byte[] pImageData)
	{
		byte[] cmd = new byte[10];
		int num;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = UP_IMAGE;
		
		num = fillPackage(sendData, CMD, 1, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -56;
		}
		return GetPackageImageUDisk( pImageData, IMAGE_X*IMAGE_Y);
	}

	public int upData2Temp( byte[] in, byte[] out )
	{
		int i = 0;
		int j = 0;
		int k = 0;
		byte[] tempData = new byte[139];
		do {
			if( in[i] == (byte)0xef )
			{
				if(in[i+1] == (byte)0x01)
				{
					if(in[i+6] == (byte)0x02)
					{
						System.arraycopy(in, i, tempData, 0, 139);
						//Log.i( TAG2, "upData2Temp 222 data = "+bytesToHexString(tempData));
						System.arraycopy(tempData, 9, out, j*128, 128);
						j++;
						i+=139;
						continue;
					}
					else if (in[i+6] == (byte)0x08) {
						System.arraycopy(in, i, tempData, 0, 139);
						//Log.i( TAG2, "upData2Temp 888 data = "+bytesToHexString(tempData));
						System.arraycopy(tempData, 9, out, j*128, 128);
						break;
					}
					else {
						k++;
						if(k>=2)
						{
							return -1;
						}
					}
				}
			}
			i++;
		} while (true);
		return 0;
	}
	
	public int upChar( int iBufferID, byte[] pTemplet, int iTempletLength)
	{
		int num;
		int ret;
		int g_nPackageSize = 0;
		byte[] tempSend = null;
		byte[] content = new byte[10];
		byte[] sendData = new byte[350];
		byte[] getData = new byte[700];
		if( iBufferID<1 || iBufferID>3 )
		{
			return -4;
		}
		content[0] = UP_CHAR;
		content[1] = (byte)iBufferID;
		
		num = fillPackage(sendData, CMD, 2, content);
		ret = SendPackageUDisk(0xffffffff, sendData);
		GetPackageCharUDisk(pTemplet);
		//if(0 != upData2Temp(getData, pTemplet))
		//{
		//	return -1;
		//}
		System.out.println( "upChar 1 = "+bytesToHexString(pTemplet));
		/*
		int totalLen = iTempletLength;
		int i = 0;
		int tempLength = 0;
		int nonius = 0;
		do{
			for( i = 0; i<350; i++)
			{
				getData[i] = (byte) 0xff;
			}
			ret = RecvCom(getData);
			if( 0 != ret )
			{
				return -1;
			}
			tempLength = GetPackageContentLength(getData) - 2;
			//ret = VerifyResponsePackage(getData[0], getData);
			//System.out.println("upChar 333 ret = "+ret);
			Log.i( TAG2, "getData = "+bytesToHexString(getData));
			if( (nonius+tempLength) > 512)
			{
				Log.i( TAG2, "nonius+tempLength error nonius="+nonius+" tempLength="+tempLength);
				return -1;
			}
			System.arraycopy( getData, 9, pTemplet, nonius, tempLength);
			nonius += tempLength;
		}while( getData[6] != ENDDATA );
		if( 0 == totalLen)
		{
			return -2;
		}
		System.out.println("upChar 555 = "+bytesToHexString(pTemplet));
		*/
		return 0;
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
	  
	public int loadChar( int iBufferID, int pageNum )
	{
		byte[] content = new byte[10];
		byte[] sendPkg = new byte[350];
		int ret = 0;

		content[0] = LOAD_CHAR;
		content[1] = (byte) (iBufferID & 0xff);
		content[2] = (byte) ((pageNum>>8) & 0xff);
		content[3] = (byte) (pageNum & 0xff);
		
		fillPackage_new(sendPkg, CMD, 4, content);
		ret = SendPackageUDisk(0xffffffff, sendPkg);
		return ret;
	}
	
	public int downChar( int iBufferID, byte[] pTemplet, int iTempletLength)
	{
		int num;
		int ret;
		byte[] content = new byte[10];
		byte[] sendData = new byte[350];
		byte[] getData = new byte[350];
		if( iBufferID<1 || iBufferID>3 || (iTempletLength<=0) )
		{
			return -4;
		}
		content[0] = DOWN_CHAR;
		content[1] = (byte)iBufferID;
		
		num = fillPackage_new(sendData, CMD, 2, content);
		ret = SendPackageUDisk(0xffffffff, sendData);
		System.out.println("downChar SendPackageCom 1 ret ="+ret);
		System.out.println("downChar SendPackageCom 1 ="+bytesToHexString(sendData));
		int totalLen = iTempletLength;
		int i = 0;
		int j = 0;
		byte[] tempTemplet = new byte[128];
		ret = SendPackageUDiskDownChar(0xffffffff, pTemplet);
		System.out.println("downChar SendPackageCom 2 ret ="+ret);
		System.out.println("downChar SendPackageCom 2 ="+bytesToHexString(pTemplet));
		/*
		for( j=0; j<4; j++)
		{
			System.arraycopy( pTemplet, 128*j, tempTemplet, 0, 128);
			if(j < 3)
			{
				for( i = 0; i<350; i++)
				{
					sendData[i] = (byte)0x00;
				}
				System.out.println("downChar totalLen="+totalLen+" tempTemplet"+bytesToHexString(tempTemplet));
				num = fillPackage_new( sendData, DATA, 128, tempTemplet);
				System.out.println("000000tempTemplet="+bytesToHexString(tempTemplet));
				ret = SendPackageUDiskDownChar(0xffffffff, sendData);
				System.out.println("downChar SendPackageCom 3 ="+bytesToHexString(sendData));
				if( 0 != ret )
				{
					System.out.println("downChar 444 ret = "+ret);
					return -1;
				}
			}
			else if (j == 3) {
				for(i=0; i<350; i++)
				{
					sendData[i] = 0x00;
				}
				num = fillPackage_new(sendData, ENDDATA, 128, tempTemplet);
				ret = SendPackageUDiskDownChar(0xffffffff, sendData);
				System.out.println("downChar SendPackageCom 4 ="+bytesToHexString(sendData));
				if( 0 != ret )
				{
					System.out.println("downChar 666");
					return -1;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		*/
		return 0;
	}
	
	public int storeChar(int bufferId, int pageId) throws IOException
	{
		byte[] content = new byte[10];
		byte[] sendPkg = new byte[350];
		
		content[0] = STORE_CHAR;
		content[1] = (byte) bufferId;
		content[2] = (byte) ((pageId>>8) & 0xff);
		content[3] = (byte) (pageId & 0xff);
		
		fillPackage(sendPkg, CMD, 4, content);
		return SendPackageUDisk(0xffffffff, sendPkg);
	}
	
	/*
	public int PSUpChar( int addr, byte bufferId )
	{
		byte[] cmd = new byte[10];
		int num;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		
		cmd[0] = UP_CHAR;
		
		num = fillPackage(sendData, CMD, 1, cmd);
		if( (ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return -56;
		}
		return GetPackageImageUDisk( pImageData, IMAGE_X*IMAGE_Y);
		
	}
	*/


	public int VerifyAutoResponsePackage(byte packageTypt, byte[] data)
	{
		if( data[0] != packageTypt){
			return -201;
		}
		int length = GetPackageLength(data);
		if( packageTypt == RESPONSE)
		{
			return 0;
		}
		return -1;
	}
	
	public int PSAutoEnroll(int addr, int bufferId, int enroll_cnt, byte[] param){

		byte cmd[] = new byte[10];

		int num;
		int ret;

		byte[] sendData = new byte[MAX_PACKAGE_SIZE];


		cmd[0] = Auto_Enroll;
		cmd[1] = (byte) (bufferId>>8 & 0xff);
		cmd[2] = (byte) (bufferId & 0xff);
		cmd[3] = (byte) enroll_cnt;
//		cmd[4] = param[0];
//		cmd[5] = param[1];
		cmd[4] = 0x00;
		cmd[5] = (byte) (~LEAVE_1 & ~STA_1 & (CTL_1 | ID_1 |  PRE_1 | LED_1));
//		cmd[5] = 0x00;

		num = fillPackage(sendData, CMD, 6, cmd);
		if( num == -1)
		{
			return -3;
		}
		if((ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return ret;
		}

		return ret;
	}

	public int PSGetEnrollSta(byte[] data){

		byte[] getData = new byte[MAX_PACKAGE_SIZE];

		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}

		if( buffer[2] == getData[2] &&  buffer[3] == getData[3] && buffer[4] == getData[4]){
			data[0] = (byte)-201;
			return -1;
		}else{
			buffer = getData;
			for(int i=0; i<3; i++){
				data[i] = getData[i+3];
			}
		}

		return 0;
	}

	public int PSAutoIdentify(int addr, int bufferId, byte[] param){
		byte cmd[] = new byte[10];

		int num;
		int ret;

		byte[] sendData = new byte[MAX_PACKAGE_SIZE];


		cmd[0] = Auto_Identify;
		cmd[1] = 0x00;
		cmd[2] = (byte) (bufferId>>8 & 0xff);
		cmd[3] = (byte) (bufferId & 0xff);
//		cmd[2] = (byte) 0xff;
//		cmd[3] = (byte) 0xff;
		cmd[4] = 0x00;
		cmd[5] = (byte) (~STA_1 & ( PRE_1 | LED_1));


		num = fillPackage(sendData, CMD, 6, cmd);
		if( num == -1)
		{
			return -3;
		}
		if((ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return ret;
		}
//		for(int i=0; i<12; i++){
//			Log.d(appName, "sendData: "+Integer.toHexString(sendData[i]) );
//		}
        buffer[3] = (byte) 0xff;

		return ret;
	}

	public int PSGetIdentifySta(byte[] data){

		byte[] getData = new byte[MAX_PACKAGE_SIZE];

		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}

		if( buffer[3] == getData[3] &&  buffer[4] == getData[4]){
			data[0] = (byte)-201;
			return -1;
		}else{
			buffer = getData;
			for(int i=0; i<6; i++){
				data[i] = getData[i+3];
			}
//			for(int i=0; i<12; i++){
//				Log.d(appName, "getData: "+Integer.toHexString(getData[i]) );
//			}
		}

		return 0;
	}

	public int PSCancel(int addr){
		byte[] cmd = new byte[10];
		int num;
		int result;
		int ret;
		byte[] sendData = new byte[MAX_PACKAGE_SIZE];
		byte[] getData = new byte[MAX_PACKAGE_SIZE];

		cmd[0] = Auto_Cancel;
		num = fillPackage(sendData, CMD, 1, cmd);
		if( num == -1)
		{
			return -3;
		}
		if((ret = SendPackageUDisk(addr, sendData)) != 0 )
		{
			return ret;
		}
		for(int i=0; i<12; i++){
			Log.d(appName, "sendData: "+Integer.toHexString(sendData[i]) );
		}
		if( 0 != GetPackageUDisk(getData) )
		{
			return -2;
		}
		for(int i=0; i<12; i++){
			Log.d(appName, "getData: "+Integer.toHexString(getData[i]) );
		}
		//Log.d(appName,"===================");
		result = VerifyResponsePackage(RESPONSE, getData);
		return result;
	}

	
	public String synoprintf()
	{
		return "hello";
	}
	
	
	
	
	

	
	
	
}
