package com.diycircuits.cangjie;

import com.diycircuits.inputmethod.latin.R;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.HashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import com.diycircuits.cangjie.CandidateSelect.CandidateListener;

public class Cangjie implements CandidateListener {

    public final static int QUICK   = 0;
    public final static int CANGJIE = 1;

    private Context mContext = null;
    private char mCodeInput[] = new char[5];
    private int  mCodeCount = 0;
    private char mCodeMap[]   = new char[26 * 2];
    private char mMatchChar[] = new char[21529];
    private int  mTotalMatch = 0;
    private TableLoader mTable = new TableLoader();
    private CandidateSelect mSelect = null;
    private CandidateListener mListener = null;

    public Cangjie(Context context) {
	mContext = context;

	ApplicationInfo appInfo = context.getApplicationInfo();
	
	for (int count = 0; count < mCodeInput.length; count++) {
	    mCodeInput[count] = 0;
	}

	try {
	    mTable.setPath(appInfo.dataDir.getBytes("UTF-8"));
	} catch (UnsupportedEncodingException ex) {
	}

	mTable.initialize();
	
	loadCangjieKey();
    }

    public boolean hasMatch() {
	return mTable.totalMatch() > 0;
    }

    public boolean isFull() {
	return mCodeCount >= mTable.getMaxKey();
    }
    
    public void setCandidateSelect(CandidateSelect select) {
	mSelect = select;
	mSelect.setCandidateListener(this);
    }

    public void setCandidateListener(CandidateListener listen) {
	mListener = listen;
    }

    public void sendFirstCharacter() {
	if (mListener != null && mTable.totalMatch() > 0) {
	    mListener.characterSelected(mTable.getMatchChar(0), 0);
	}
	resetState();
    }

    public char getFirstCharacter() {
	return mTable.getMatchChar(0);
    }
    
    public void characterSelected(char c, int idx) {
	if (mListener != null) mListener.characterSelected(c, idx);
	resetState();
    }

    public void resetState() {
	mTable.reset();
	for (int count = 0; count < mCodeInput.length; count++) {
	    mCodeInput[count] = 0;
	}
	mCodeCount = 0;
	mSelect.updateMatch(null, 0);
	mSelect.closePopup();
    }
    
    private void loadCangjieKey() {
	try {
	    InputStream is = mContext.getResources().openRawResource(R.raw.cj_key);
	    InputStreamReader input = new InputStreamReader(is, "UTF-8");
	    BufferedReader reader = new BufferedReader(input);
	    String str = null;
	    int count = 0, index = 0;
	    char c = 'a';
	
	    do {
		str = reader.readLine();
		mCodeMap[count + 1] = str.charAt(0);
		mCodeMap[count    ] = str.charAt(2);
		count += 2;
	    } while (str != null && count < mCodeMap.length);
		    
	    reader.close();

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
    
    private char convertPrimaryCode(int primaryCode) {
	for (int count = 0; count < mCodeMap.length; count += 2) {
	    if (mCodeMap[count] == primaryCode)
		return mCodeMap[count + 1];
	}

	return 0;
    }
    
    public void handleCharacter(int primaryCode) {
	if (mCodeCount >= 5) return;
	char code = convertPrimaryCode(primaryCode);
	if (code == 0) return;
	mCodeInput[mCodeCount++] = code;
	matchCangjie();
    }
    
    public void deleteLastCode() {
	if (mCodeCount <= 0) return;
	mCodeInput[--mCodeCount] = 0;

	if (mCodeCount == 0) {
	    mTable.reset();
	    mSelect.updateMatch(null, 0);
	} else { 
	    matchCangjie();
	}
    }

    private boolean matchCangjie() {
	boolean res = mTable.tryMatchCangjie(mCodeInput[0], mCodeInput[1], mCodeInput[2], mCodeInput[3], mCodeInput[4]);

	if (res) {
	    mTable.searchCangjie(mCodeInput[0], mCodeInput[1], mCodeInput[2], mCodeInput[3], mCodeInput[4]);
	    for (int count = 0; count < mTable.totalMatch(); count++) {
		mMatchChar[count] = mTable.getMatchChar(count);
	    }
	    mSelect.updateMatch(mMatchChar, mTable.totalMatch());
	}
	
	return res;
    }
    
}

