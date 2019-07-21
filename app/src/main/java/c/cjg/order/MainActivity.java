package c.cjg.order;

/*xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 Written by Cris Graue starting April 2018
 06/19/2018 ver 0.5
 07/10/2018 ver 0.9 edit/load/save only limited data fields
 09/02/2018 ver 1.0 menu expansion
 09/10/2018 ver 1.1 Interface rework
 09/22/2018 ver 1.2 generate CSV of order
 07/06/2019 ver 1.5 rebuild Order from CJGOrder (rename)
 xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx*/

//import com.cjg.cjglib;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class MainItem  {
    String item;                //numeric digit         itemLen
    String descr;               //all caps text         descrLen
    Integer qoh;                //number on hand        qohLen chars
    Integer qty;                //number to order       qtyLen chars / qtyLimit numeric limit
    String qtyBy;               //unit each/case/lb     qtyByLen
    String UPC;                 //code (match scan)     UPCLen
    String keys;                //text                  keysLen
    Integer groupMaj, groupMin;
    String ADate;
    Integer par;
    String Extra;

    public String getItem() {
        return item;
    }
    public String getDescr() {
        return descr;
    }
    public Integer getQoh() {
        return qoh;
    }
    public Integer getQty() {
        return qty;
    }
    public String getQtyBy() {
        return qtyBy;
    }
    public String getUPC() {
        return UPC;
    }
    public String getKeys() {
        return keys;
    }
    public int getMaj() {
        return groupMaj;
    }
    public int getMin() {
        return groupMin;
    }
    public String getADate() {
        return ADate;
    }
    public int getPar() {
        return par;
    }
    public String getExtra() {
        return Extra;
    }
}
class OrderItem {
    String Item;
    Integer Qty;
    public String getItem(){return Item;}
    public Integer getQty() {return Qty;}
}
class mViewHolder {
    TextView item;
    TextView descr;
    TextView qoh;
    TextView qty;
    TextView qtyby;
    TextView keys;
    TextView extra;
}
class oViewHolder {
    TextView item;
    TextView descr;
    TextView qoh;
    TextView qty;
}

public class MainActivity extends AppCompatActivity {
    static final int itemLen = 5, descrLen = 30, qohLen = 4, qtyLen = 4, qLimit = 9999, qtyByLen = 10, parLen = 3,
            gMajLen = 3, gMinLen = 3, UPCLen = 15, keysLen = 40, AdateLen = 8;
    public String mPath = "", addFile = "", iPath = "";
    public TextView itemView = null, ADateView = null;
    public EditText seekView = null, descrView = null, qohView = null, qtyView = null, qtyByView = null, parView = null,
            gmajView = null, gminView = null, UPCView = null, keysView = null, extraView = null, fView = null;
    public Integer mnext = 0;
    public int mIX = -1, oIX = -1, mainV = 0, mCent=150;
    private Toast toast;
    private MainItem mainItem;
    public ListView mlv = null;
    private ListAdapter mla;
    private OListAdapter ola;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainV = 0;
        setContentView(R.layout.activity_main);
        mPath = this.getObbDir().toString();
        iPath = this.getExternalFilesDir(null).toString();
//        fView = findViewById(R.id.fname)
//        fView.setText(addFile);

//        odump();       //debug data file create
        mlv = this.findViewById(R.id.mainList);
//        mlv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//        mlv.setEmptyView(findViewById(R.id.list_empty));
        mla = new ListAdapter();
        mLoad(mPath +"/"+getResources().getString(R.string.main_file), 0);  //0 is clear array first  "NEW"
        mlv.setAdapter(mla);
        if (savedInstanceState != null) {
//            simpleToast("Restore Instance", 5);
            mIX = (int) savedInstanceState.get("mIX");
            mainV = (int) savedInstanceState.get("mainV");
            if (mainV == 1) editMain();
        }
        if ((mIX > -1) && (mIX < mla.getCount())) mlv.setSelection(mIX);
        mla.notifyDataSetChanged();
//        mlv.requestFocus();
        mlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                mIX = position;
                mlv.setSelection(mIX);
                mla.notifyDataSetChanged();
            }
        });
//        if (mainV==0) {
//            seekView = findViewById(R.id.seekItem);
//            seekView.requestFocus();
//        }
//        simpleToast("End onCreate", 1);
    }

    /*
        @Override
        protected void onResume() {
            super.onResume();
            simpleToast("onResume", 1);
        }
    */
    @Override
    public void onSaveInstanceState(Bundle outState) {
//        simpleToast("save instance", 1);
        super.onSaveInstanceState(outState);
        outState.putInt("mIX", mIX);
        outState.putInt("mainV", mainV);
//        View w = getCurrentFocus();
//        outState.putParcelable("curView", (Parcelable) w);
        if (mSave(getResources().getString(R.string.main_file)) == -1) simpleToast("Error saving Master!", 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file, menu);
        getMenuInflater().inflate(R.menu.menu_entry, menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Integer rslt = 0;

        switch (id) {
            case R.id.action_settings:
                simpleToast("Settings...", 1);
                return true;
            case R.id.action_net:
                simpleToast("action network", 0);
//            Intent intent = new Intent(MainActivity.this, com.cjg.cjgorder.cjgnet.class);
            /*          intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
            intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());
            intent.putExtra(com.bt.cjgbt.ui.PeripheralControlActivity.EXTRA_TYPE, device.getType().toString()));*/
//            startActivity(intent);
//              simpleToast("Activity cjgnet started",15);
                return true;
            case R.id.action_exit:
                rslt = mSave(getResources().getString(R.string.main_file));
                if (rslt > -1)
                    simpleToast(rslt + " entries saved.", 1);
                else
                    simpleToast("Error saving Master!", 1);
                finish();
                return true;
            case R.id.action_quit:
                simpleToast("Changes discarded.", 0);
                finish();
                return true;
            case R.id.entry_find:
                onFindButton(this.getCurrentFocus());
                return true;
            case R.id.file_add:
//              simpleToast("add file",0);
                mLoad(mPath +"/"+getResources().getString(R.string.add_file), 1);    // don't clear array
                mla.notifyDataSetChanged();
                return true;
            case R.id.file_import:
                String tmp = iPath +"/"+getResources().getString(R.string.import_file);
                simpleToast("import file - "+tmp,0);
                mLoad(tmp, 1);    // don't clear array
                mla.notifyDataSetChanged();
                return true;
            case R.id.file_csv:
//              simpleToast("gen CSV file",0);
                rslt = genCSV(getResources().getString(R.string.csv_file));
                if (rslt > -1)
                    simpleToast(rslt + " entries in order.", 1);
                else
                    simpleToast("Error generating CSV!", 1);
                return true;
            case R.id.file_save:
//              simpleToast("save file",0);
                rslt = mSave(getResources().getString(R.string.main_file));
                if (rslt > -1)
                    simpleToast(rslt + " entries saved.", 1);
                else
                    simpleToast("Error saving Master!", 1);
                return true;
            case R.id.file_sample:
//              simpleToast("dump file",0);
                odump();
                return true;
            case R.id.file_backup:
                String fname = getResources().getString(R.string.backup_file) + cStamp(Calendar.getInstance().getTime());
                rslt = mSave(fname);
                if (rslt > -1)
                    simpleToast(rslt + " entries saved.", 1);
                else
                    simpleToast("Error saving Backup!", 1);
                return true;
            case R.id.entry_edit:
                onEditButton(getCurrentFocus());
                return true;
            case R.id.entry_zeroQty:
                zeroOrder();
                return true;
            case R.id.entry_setPar:
                setToPar();
                return true;
            case R.id.entry_delete:
                if (mainV == 0) {     //don't delete during edit, only maim view
                    simpleToast("Entry " + mla.get(mIX).item + " " + mla.get(mIX).descr + " deleted.", 3);
                    mla.remove(mIX);
                    if (mIX > -1) mIX--;
                    mla.notifyDataSetChanged();
                } else
                    simpleToast("ONLY from main window", 0);
                return true;
            case R.id.action_about:
                simpleToast(getResources().getString(R.string.about_text),
                        Integer.parseInt(getResources().getString(R.string.about_display_duration)));
                return true;
            case R.id.action_break:
                simpleToast("Breakpoint", 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onNewButton(View view) {
        kboff();
        seekView = findViewById(R.id.seekItem);
        String item = seekView.getText().toString().trim(); //see what was left in the box
        if (item.length() > 0) {     // not blank...
            mIX = mla.look(item);                                     //see if there's a main detail for it
            if (mIX < 0) {                        //no main entry, make one now
                mainItem = new MainItem();
                mainItem.item = item;
                mainItem.descr = "";
                mainItem.qty = 0;
                mainItem.qoh = 0;
                mainItem.qtyBy = "";
                mainItem.par = 0;
                mainItem.groupMaj = 0;
                mainItem.groupMin = 0;
                mainItem.UPC = "";
                mainItem.keys = "";
                mainItem.Extra = "";
                mainItem.ADate = cStamp(Calendar.getInstance().getTime());
                mIX = mla.add(mainItem);
            }
            editMain();                                          // edit entry
        }
    }
    public void onFindButton(View view) {
        kboff();
        seekView = findViewById(R.id.seekItem);
        String item = seekView.getText().toString().trim(); //see what was left in the box
        if (item.length() > 0) {     // not blank...
            mIX = mla.look(item);                                     //see if there's a main detail for it
            if (mIX < 0)
                simpleToast("no entry", 0);
            else {                               //just move selection to existing entry, don't edit
                mla.notifyDataSetChanged();
                mlv.setSelection(mIX);

            }
        }
    }
    public void onMinusButton(View view) {
        view.requestFocus();
        if (mla.get(mIX).qty > 0) {
            mla.get(mIX).qty--;
//            qtyView = findViewById(R.id.xQty);
//            qtyView.setText(String.format("%d", mla.get(mIX).qty));
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
//        simpleToast("-",1);
    }
    public void onPlusButton(View view) {
        view.requestFocus();
        if (mla.get(mIX).qty < qLimit) {
            mla.get(mIX).qty++;
//            qtyView = findViewById(R.id.xQty);
//            qtyView.setText(String.format("%d", mla.get(mIX).qty));
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
//        simpleToast("+",1);
    }
    public void onqMinusButton(View view) {
        view.requestFocus();
        if (mla.get(mIX).qoh > 0) {
            mla.get(mIX).qoh--;
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
//        simpleToast("qoh-",1);
    }
    public void onqPlusButton(View view) {
        view.requestFocus();
        if (mla.get(mIX).qoh < qLimit) {
            mla.get(mIX).qoh++;
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
//        simpleToast("qoh+",1);
    }
    public void onUpButton(View view) {
//        simpleToast("up",1);
        if (mIX > 0) {
            mainItem = mla.get(mIX);
            mIX--;
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
    }
    public void onDownButton(View view) {
//        simpleToast("Down",1);
        if (mIX < mla.getCount() - 1) {
            mainItem = mla.get(mIX);
            mIX++;
            mlv.setSelection(mIX);
            mla.notifyDataSetChanged();
        }
    }
    public void sUpButton(View view) {
        if (mIX > 0) {
            mainItem = mla.get(mIX - 1);
            mla.set(mIX - 1, mla.get(mIX));
            mla.set(mIX, mainItem);
            mIX--;
            mla.notifyDataSetChanged();
            mlv.setSelectionFromTop(mIX,mCent);

        }
    }
    public void sDownButton(View view) {
        if (mIX < mla.getCount() - 1) {
            mainItem = mla.get(mIX + 1);
            mla.set(mIX + 1, mla.get(mIX));
            mla.set(mIX, mainItem);
            mIX++;
            mla.notifyDataSetChanged();
            mlv.setSelectionFromTop(mIX,mCent);

        }
    }
    public void onEditButton(View view) {
        editMain();
    }
    public void onDoneButton(View view) {
//        simpleToast("Done",1)
        mla.get(mIX).descr = descrView.getText().toString().trim();
        mla.get(mIX).qty = text2num(qtyView.getText().toString());
        mla.get(mIX).qtyBy = qtyByView.getText().toString().trim();
        mla.get(mIX).par = text2num(parView.getText().toString());
        mla.get(mIX).groupMaj = text2num(gmajView.getText().toString());
        mla.get(mIX).groupMin = text2num(gminView.getText().toString());
        mla.get(mIX).qoh = text2num(qohView.getText().toString());
        mla.get(mIX).UPC = UPCView.getText().toString().trim();
        mla.get(mIX).keys = keysView.getText().toString().trim();
        mla.get(mIX).Extra = extraView.getText().toString().trim();
        mainV = 0;
        setContentView(R.layout.activity_main);
        mlv = this.findViewById(R.id.mainList);
        mlv.setAdapter(mla);
        mlv.setSelection(mIX);
        mlv.requestFocus();
        mla.notifyDataSetChanged();
        mlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                mIX = position;
                mlv.setSelection(mIX);
                mla.notifyDataSetChanged();
            }
        });
    }
    public void editMain() {
        mainV = 1;
        setContentView(R.layout.item_main);
        itemView = findViewById(R.id.xItem);
        descrView = findViewById(R.id.xDescr);
        qohView = findViewById(R.id.xQoh);
        qtyView = findViewById(R.id.xQty);
        qtyByView = findViewById(R.id.xQtyBy);
        parView = findViewById(R.id.xPar);
        gmajView = findViewById(R.id.xgMaj);
        gminView = findViewById(R.id.xgMin);
        UPCView = findViewById(R.id.xUPC);
        keysView = findViewById(R.id.xkeys);
        extraView = findViewById(R.id.xExtra);
        ADateView = findViewById(R.id.xADate);
        if ((mIX > -1) && (mIX < mla.getCount())) {             //must be in array range
            itemView.setText(mla.get(mIX).item.trim());
            descrView.setText(mla.get(mIX).descr.trim());
            qohView.setText(num2text(mla.get(mIX).qoh));
            qtyView.setText(num2text(mla.get(mIX).qty));
            qtyByView.setText(mla.get(mIX).qtyBy.trim());
            parView.setText(num2text(mla.get(mIX).par));
            gmajView.setText(num2text(mla.get(mIX).groupMaj));
            gminView.setText(num2text(mla.get(mIX).groupMin));
            UPCView.setText(mla.get(mIX).UPC.trim());
            keysView.setText(mla.get(mIX).keys.trim());
            ADateView.setText(mla.get(mIX).ADate.trim());
        } else {                                             //not valid so show blank... change color???
            itemView.setText("");
            descrView.setText("");
            qtyView.setText("");
            qohView.setText("");
            qtyByView.setText("");
            gmajView.setText("");
            gminView.setText("");
            UPCView.setText("");
            keysView.setText("");
            extraView.setText("");
            ADateView.setText("");
        }
    }
    public void setToPar() {
        int i = 0, q = 0, qc = 0;
        while (i < mla.getCount()) {
            q = mla.get(i).par - mla.get(i).qoh;
            if (q > 0) {
                mla.get(i).qty = q;
                qc++;
            } else if (q < 0)
                simpleToast(mla.get(i).item + " " + mla.get(i).descr + " over par!", 1);
            i++;
        }
        simpleToast(q + " items set.", 1);
        mla.notifyDataSetChanged();
    }
    public void zeroOrder() {
        int i = 0;
        while (i < mla.getCount()) {
            mla.get(i).qty = 0;
            i++;
        }
        simpleToast("Order set to zero.", 1);
        mla.notifyDataSetChanged();
    }
    public void editOrder() {
        mainV = 2;
        mlv = this.findViewById(R.id.mainList);
//        mlv.setEmptyView(findViewById(R.id.list_empty));
        ola = new OListAdapter();
        mlv.setAdapter(ola);
        oIX = 0;
        int ixm = 0;
        while (ixm < mla.getCount()) {
            MainItem temp = mla.get(ixm);
            if (temp.getQty() > 0) {
                OrderItem entry = new OrderItem();
                entry.Item = temp.getItem();
                entry.Qty = temp.getQty();
                ola.add(entry);
            }
            ixm++;
        }
        if ((oIX > -1) && (oIX < mla.getCount())) mlv.setSelection(oIX);
        ola.notifyDataSetChanged();
        mlv.requestFocus();
        mlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // entry selected from listview of items
                if (toast != null) toast.cancel();
                oIX = position;
                mlv.setSelection(oIX);
                ola.notifyDataSetChanged();
            }
        });
        seekView = findViewById(R.id.seekItem);
        seekView.requestFocus();

    }
    public void mToPos(int pos) {
        // save index and top position
        int index = mlv.getFirstVisiblePosition();
        View v = mlv.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - mlv.getPaddingTop());

// ...

// restore index and position
        mlv.setSelectionFromTop(pos, top);
    }
    private void mLoad(String fname, int mode) {
        BufferedReader br;
        String line;
        int len;
        int from;
        mnext = 0;
        // Read file
        try {
            br = new BufferedReader(new FileReader(fname));
            if (mode == 0) {         //start fresh
                mla.clear();
                mIX = -1;
            }
            while ((line = br.readLine()) != null) {
                len = line.length();
                from = 0;
                String s = line.substring(0, itemLen).trim();
                if (mla.look(s) < 0) {
                    mainItem = new MainItem();
                    if (len >= itemLen) {
                        mainItem.item = s;
                        from = itemLen;
                    } else mainItem.item = "";
                    if (len >= from + descrLen) {
                        mainItem.descr = line.substring(from, from + descrLen).trim();
                        from = from + descrLen;
                    } else mainItem.descr = "";
                    if (len >= from + qtyByLen) {
                        mainItem.qtyBy = line.substring(from, from + qtyByLen).trim();
                        from = from + qtyByLen;
                    } else mainItem.qtyBy = "";
                    if (len >= from + qtyLen) {
                        mainItem.qty = text2num(line.substring(from, from + qtyLen));
                        from = from + qtyLen;
                    } else mainItem.qty = 0;
                    if (len >= from + parLen) {
                        mainItem.par = text2num(line.substring(from, from + parLen));
                        from = from + parLen;
                    } else mainItem.par = 0;
                    if (len >= from + gMajLen) {
                        mainItem.groupMaj = text2num(line.substring(from, from + gMajLen));
                        from = from + gMajLen;
                    } else mainItem.groupMaj = 0;
                    if (len >= from + gMinLen) {
                        mainItem.groupMin = text2num(line.substring(from, from + gMinLen));
                        from = from + gMinLen;
                    } else mainItem.groupMin = 0;
                    if (len >= from + UPCLen) {
                        mainItem.UPC = line.substring(from, from + UPCLen).trim();
                        from = from + UPCLen;
                    } else mainItem.UPC = "";
                    if (len >= from + keysLen) {
                        mainItem.keys = line.substring(from, from + keysLen).trim();
                        from = from + keysLen;
                    } else mainItem.keys = "";
                    if (len >= from + AdateLen) {
                        mainItem.ADate = line.substring(from, from + AdateLen).trim();
                        from = from + AdateLen;
                    } else mainItem.ADate = "";
                    if (len >= from + qohLen) {
                        mainItem.qoh = text2num(line.substring(from, from + qohLen));
                        from = from + qohLen;
                    } else mainItem.qoh = 0;
                    mainItem.Extra = line.substring(from, len).trim();
                    if (mainItem.Extra == null) mainItem.Extra = "";
                    mIX = mla.add(mainItem);
                } else simpleToast(s + " exists", 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            simpleToast("IO Exception" + e, 1);
        }
    }
    private Integer mSave(String fname) {
        String filetext = "", line;
        mIX = 0;
        while (mIX < mla.getCount()) {
            mainItem = mla.get(mIX++);
            if (mainItem.item.length()>itemLen) line = mainItem.item.substring(0,itemLen);
            else line = mainItem.item;
            while (line.length() < itemLen) line = "0" + line;
            filetext = filetext + line;
            if (mainItem.descr.length()>descrLen) line = mainItem.descr.substring(0,descrLen);
            else line = mainItem.descr;
            while (line.length() < descrLen) line = line + " ";
            filetext = filetext + line;
            if (mainItem.qtyBy.length()>qtyByLen) line = mainItem.qtyBy.substring(0,qtyByLen);
            else line = mainItem.qtyBy;
            while (line.length() < qtyByLen) line = line + " ";
            filetext = filetext + line;
            if (mainItem.qty == null) line = "0";
            else line = mainItem.qty.toString();
            while (line.length() < qtyLen) line = "0" + line;
            filetext = filetext + line;
            if (mainItem.par == null) line = "0";
            else line = mainItem.par.toString();
            while (line.length() < parLen) line = "0" + line;
            filetext = filetext + line;
            if (mainItem.groupMaj == null) line = "0";
            else line = mainItem.groupMaj.toString();
            while (line.length() < gMajLen) line = "0" + line;
            filetext = filetext + line;
            if (mainItem.groupMin == null) line = "0";
            else line = mainItem.groupMin.toString();
            while (line.length() < gMinLen) line = "0" + line;
            filetext = filetext + line;
            if (mainItem.UPC.length()>UPCLen) line = mainItem.UPC.substring(0,UPCLen);
            else line = mainItem.UPC;
            while (line.length() < UPCLen) line = line + " ";
            filetext = filetext + line;
            if (mainItem.keys.length()>keysLen) line = mainItem.keys.substring(0,keysLen);
            else line = mainItem.keys;
            while (line.length() < keysLen) line = line + " ";
            filetext = filetext + line;
            if (mainItem.ADate.length()>AdateLen) line = mainItem.ADate.substring(0,AdateLen);
            else line = mainItem.ADate;
            while (line.length() < AdateLen) line = line + " ";
            filetext = filetext + line;
            if (mainItem.qoh == null) line = "0";
            else line = mainItem.qoh.toString();
            while (line.length() < qohLen) line = "0" + line;

            filetext = filetext + line + mainItem.Extra + System.lineSeparator();
        }
        try {
            File file = new File(mPath +"/"+fname);
            if (!file.exists()) file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(filetext);
            bw.close();
            Log.d("Suceess", "Table save Sucess");
            return mIX+1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
    private Integer genCSV(String fname) {
        String filetext = "", line;
        int oq=0;
        mIX = 0;
        while (mIX < mla.getCount()) {
            mainItem = mla.get(mIX++);
            if (mainItem.qty > 0) {     //only include QTY greater than 0
                if (mainItem.item.length() > itemLen)
                    line = mainItem.item.substring(0, itemLen);
                else
                    line = mainItem.item;
                line = line + "," + mainItem.qty.toString();
                filetext = filetext + line + System.lineSeparator();
                oq++;
            }
        }
        if (filetext.length()>0)                    //got something to write...
            try {
                File file = new File(mPath +"/"+fname);
                if (!file.exists()) file.createNewFile();
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(filetext);
                bw.close();
                Log.d("Suceess", "CSV save Sucess");
                return oq;
            } catch (IOException e) { e.printStackTrace(); };
        return -1;
    }
    public void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    public void kboff() {
        InputMethodManager inputManager =
                (InputMethodManager) this.
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }
    public String cStamp(Date fullTime) {

        String fullStamp = fullTime.toString();
        String year = fullStamp.substring(24, 28);
        String mo = fullStamp.substring(4, 7);
        String month = "00";
        switch (mo) {
            case "Jan":
                month = "01";
                break;
            case "Feb":
                month = "02";
                break;
            case "Mar":
                month = "03";
                break;
            case "Apr":
                month = "04";
                break;
            case "May":
                month = "05";
                break;
            case "Jun":
                month = "06";
                break;
            case "Jul":
                month = "07";
                break;
            case "Aug":
                month = "08";
                break;
            case "Sep":
                month = "09";
                break;
            case "Oct":
                month = "10";
                break;
            case "Nov":
                month = "11";
                break;
            case "Dec":
                month = "12";
                break;
        }
        String day = fullStamp.substring(8, 10);
//        String sTime = fullStamp.substring(11, 19);
        String mStamp = year + month + day;   // + " " + sTime;
        return mStamp;
    }
    public String num2text(Integer i) {                    //blank in EditText shows "Hint" field
        if (i != 0)
//                 String.format                 %[argument_index$][flags][width][.precision]conversion
            return String.format("%d",i);
        else
            return "";
    }
    public Integer text2num(String seg) {
        Integer r = 0;
        if ((seg==null)||seg.equals(""))
            r = 0;
        else {
            try {
                r = Integer.parseInt(seg);
            } catch (NumberFormatException e) {
                r = 0;
                e.printStackTrace();
                simpleToast("NumberFormatException " + e, 1);
            }
        }
        return r;
    }
    private class ListAdapter extends BaseAdapter {
        private ArrayList<MainItem> mItems;

        public ListAdapter() {
            super();
            mItems = new ArrayList<>();
        }

        public int add(MainItem entry) {
            if (!mItems.contains(entry))
                mItems.add(entry);            //if not there, add it
            return mItems.indexOf(entry);
        }

        public int look(String str) {
            int pos = 0;
            while (pos < mItems.size()) {
                if (mItems.get(pos).item.equals(str))
                    return pos;
                else
                    pos++;
            }
            return -1;
        }

        public void remove(int i) {
            mItems.remove(i);
        }

        public void clear() {
            mItems.clear();
        }

        public MainItem get(int i) {
            return mItems.get(i);
        }

        public MainItem set(int pos, MainItem entry) {
            return mItems.set(pos, entry);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int i) {
            return mItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            mViewHolder mvh;
            if (convertView == null) {
                convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, viewGroup, false);  //Row view
                mvh = new mViewHolder();
                mvh.item = convertView.findViewById(R.id.lItem);
                mvh.descr = convertView.findViewById(R.id.lDesc);
                mvh.qoh = convertView.findViewById(R.id.lQoh);
                mvh.qty = convertView.findViewById(R.id.lQty);
                mvh.qtyby = convertView.findViewById(R.id.lQtyBy);
                mvh.keys = convertView.findViewById(R.id.lKeys);
                mvh.extra = convertView.findViewById(R.id.lExtra);
                convertView.setTag(mvh);
            } else {
                mvh = (mViewHolder) convertView.getTag();
            }
//Fill view with the values from data source
            mvh.item.setText(mItems.get(i).item);
            mvh.item.setSelected(mItems.get(i).qty>0);
            mvh.item.setId(i);
            mvh.descr.setText(mItems.get(i).descr);
            mvh.descr.setSelected(i==mIX);
            mvh.descr.setId(i);
            mvh.qoh.setText(String.format("%d", mItems.get(i).qoh));
            mvh.qoh.setSelected(i==mIX);
            mvh.qoh.setId(i);
            mvh.qty.setText(String.format("%d", mItems.get(i).qty));
            mvh.qty.setSelected((mItems.get(i).qty>0));
            mvh.qty.setId(i);
            mvh.qtyby.setText(mItems.get(i).qtyBy);
            mvh.qtyby.setSelected(i==mIX);
            mvh.qtyby.setId(i);
            mvh.keys.setText(mItems.get(i).keys);
            mvh.keys.setSelected(i==mIX);
            mvh.keys.setId(i);
            mvh.extra.setText(mItems.get(i).Extra);
            mvh.extra.setSelected(i==mIX);
            mvh.extra.setId(i);
            return convertView;
        }
    }
    private class OListAdapter extends BaseAdapter {
        private ArrayList<OrderItem> oItems;

        public OListAdapter() {
            super();
            oItems = new ArrayList<>();
        }

        public int add(OrderItem entry) {
            if (!oItems.contains(entry))
                oItems.add(entry);            //if not there, add it
            return oItems.indexOf(entry);
        }

        public int look(String str) {
            int pos = 0;
            while (pos < oItems.size()) {
                if (oItems.get(pos).Item.equals(str))
                    return pos;
                else
                    pos++;
            }
            return -1;
        }

        public void remove(int i) {
            oItems.remove(i);
        }

        public void clear() {
            oItems.clear();
        }

        public OrderItem get(int i) {
            return oItems.get(i);
        }

        public OrderItem set(int pos, OrderItem entry) {
            return oItems.set(pos, entry);
        }

        @Override
        public int getCount() {
            return oItems.size();
        }

        @Override
        public Object getItem(int i) {
            return oItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            oViewHolder ovh;
            if (convertView == null) {
                convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, viewGroup, false);  //Row view
                ovh = new oViewHolder();
                ovh.item = convertView.findViewById(R.id.lItem);
                ovh.descr = convertView.findViewById(R.id.lDesc);
                ovh.qoh = convertView.findViewById(R.id.lQoh);
                ovh.qty = convertView.findViewById(R.id.lQty);
                convertView.setTag(ovh);
            } else {
                ovh = (oViewHolder) convertView.getTag();
            }
//Fill view with the values from data source
            ovh.item.setText(oItems.get(i).Item);
            ovh.item.setSelected((i==mIX));
            ovh.item.setId(i);
            //          ovh.descr.setText(oItems.get(i).descr);
            ovh.descr.setSelected((i==mIX));
            ovh.descr.setId(i);
//            ovh.qoh.setText(String.format("%d", oItems.get(i).qoh));
            ovh.qoh.setSelected((i==mIX));
            ovh.qoh.setId(i);
            ovh.qty.setText(String.format("%d", oItems.get(i).Qty));
            ovh.qty.setSelected((i==mIX));
            ovh.qty.setId(i);
            return convertView;
        }
    }

    private void odump() {
        String filetext =
                "52976SODA COKE HY BIB              0004BAG IN BOX002002001UPC            beverage                                201805010001stuff here\n" +
                        "79391HAMBURGER BUN WHEAT OAT TOPPED0001CASE BAGS 001002002UPC            bread                                   201805010000extra stuff\n" +
                        "68462BASE STRAWBERRY FRUIT CREATION0001CASE      006001002UPC            beverage                                201805010000extra stuff\n" +
                        "35923MUSTARD SQUEEZE BOTTLE 12/12OZ0000CASE      000000000                                                       201805010000extra stuff\n" +
                        "18507TEA HERBAL ORG GARDEN CAF FREE0000          000000000                                                       201805010000\n" +
                        "49029SPOON SOUP BLK PLASTIC WRAPPED0003CASE      000000000               non-food                                201805010000extra stuff\n" +
                        "68462BASE STRAWBERRY FRUIT CREATION0000          000000000                                                       201805010000\n" +
                        "86554COASTER 4IN ORANGE/BROWN BANDS0000          000000000                                                       201805010000a stuff\n" +
                        "73991HAM SHAVED DELI 3 OZ PORTIONS 0001CASE BAGS 000000000               meat                                    201805010000\n" +
                        "76509BREAD MULTIGRAIN/WHEAT SLICED 0000          000000000                                                       201805010000\n" +
                        "94191ALL PURPOSE SEASON SPICE TRADE0001          000000000                                                       201805010000\n" +
                        "61937FRIES WAVELENGTH              0003          000000000                                                       201805010000/\n" +
                        "79391HAMBURGER BUN WHEAT OAT TOPPED0000CASE BAGS 000000000               bread                                   201805010000\n" +
                        "94258PAPER WAX PATTY 6X6IN         0007BOX SHEETS001005005UPC            non-food                                201805010001extra stuff\n" +
                        "18912SINGLE SERV CREAMER FRCH VAN  0000          000000000                                                       201805010000\n" +
                        "65327SOURDOUGH BREAD SLICED        0000CASE BAGS 000000000               bread                                   201805010000\n" +
                        "50387LIQUID EGGS                   0004          000000000                                                       201805010000\n" +
                        "99756LID 12 16 24OZ PLAS CUP FLAT  0000          000000000               keys                                    201805010000a stuff\n" +
                        "18667JOURNAL PAPER 3.1INX273 THERM 0001          000000000                                                       201805010000\n" +
                        "53822KETCHUP 20 OZ BOTTLE HEINZ    0000          000000000                                                       201805010000\n" +
                        "76544SYRUP PLAIN CANE DRY          0000          000000000                                                       201805010000\n" +
                        "76599SUGAR PC DENNYS 10 OZ         0009          000000000                                                       201805010000\n" +
                        "49033BAG SINGLE ORDER DOD LOGO     0001CASE      002990099UPC            non-food                                201805010000extra stuff\n" +
                        "61907NAPKIN 17X17 1 PLY 1/4 FOLD   0002CASE BAGS 002011022               keys                                    201805010010extra stuff\n" +
                        "99999123456789A123456789B123456789C9999PACKAGINGX009111222UPC456789012345keys                                    201805010002extra stuff here\n";
        File file = new File(mPath +"/"+getResources().getString(R.string.main_file));
        try {
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(filetext);
            bw.close();
        } catch (IOException e) {
            simpleToast("fail", 9);
        }
        finish();
    }
}
/*  fancy string input popup
        // Container layout to hold other components
        LinearLayout llContainer = new LinearLayout(this);
        // Set its orientation to vertical to stack item
        llContainer.setOrientation(LinearLayout.VERTICAL);
        // Container layout to hold EditText and Button
        LinearLayout llContainerInline = new LinearLayout(this);
        // Set its orientation to horizontal to place components next to each other
        llContainerInline.setOrientation(LinearLayout.HORIZONTAL);
        // EditText to get input
        final EditText etInput = new EditText(this);
        // TextView to show an error message when the user does not provide input
        final TextView tvError = new TextView(this);
        // For when the user is done
        Button bDone = new Button(this);
// If tvError is showing, make it disappear
        etInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvError.setVisibility(View.GONE);
            }
        });
        // This is what will show in etInput when the Popup is first created
        etInput.setHint("Enter item number");
        // Input type allowed: Numbers
        etInput.setRawInputType(Configuration.KEYBOARD_12KEY);
        // Center text inside EditText
        etInput.setGravity(Gravity.CENTER);
        // tvError should be invisible at first
        tvError.setVisibility(View.GONE);
        bDone.setText("Done");
        bDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If user didn't input anything, show tvError
                if (etInput.getText().toString().equals("")) {
                    tvError.setText("Please enter a valid value");
                    tvError.setVisibility(View.VISIBLE);
                    etInput.setText("");
                    // else, return string input
                } else {
                    popupWindow.dismiss();
                    return etInput.getText().toString();
                }
            }
        });
        // Define LayoutParams for tvError
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 20;
        // Define LayoutParams for InlineContainer
        LinearLayout.LayoutParams layoutParamsForInlineContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParamsForInlineContainer.topMargin = 30;
        // Define LayoutParams for EditText
        LinearLayout.LayoutParams layoutParamsForInlineET = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        // Set ET's weight to 1 // Take as much space horizontally as possible
        layoutParamsForInlineET.weight = 1;
        // Define LayoutParams for Button
        LinearLayout.LayoutParams layoutParamsForInlineButton = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        // Set Button's weight to 0
        layoutParamsForInlineButton.weight = 0;
        // Add etInput to inline container
        llContainerInline.addView(etInput, layoutParamsForInlineET);
        // Add button with layoutParams // Order is important
        llContainerInline.addView(bDone, layoutParamsForInlineButton);
        // Add tvError with layoutParams
        llContainer.addView(tvError, layoutParams);
        // Finally add the inline container to llContainer
        llContainer.addView(llContainerInline, layoutParamsForInlineContainer);
        // Set gravity
        llContainer.setGravity(Gravity.CENTER);
        // Set any color to Container's background
        llContainer.setBackgroundColor(0x95000000);
        // Create PopupWindow
        popupWindow = new PopupWindow(llContainer,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // Should be focusable
        popupWindow.setFocusable(true);
        // Show the popup window
        popupWindow.showAtLocation(llContainer, Gravity.CENTER, 0, 0);
    }
*/