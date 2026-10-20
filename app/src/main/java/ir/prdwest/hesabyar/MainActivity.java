package ir.prdwest.hesabyar;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import android.content.SharedPreferences;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
  final int blue=Color.rgb(37,99,235), green=Color.rgb(5,150,105), red=Color.rgb(225,29,72), purple=Color.rgb(124,58,237), ink=Color.rgb(15,23,42);
  SharedPreferences prefs; ArrayList<Tx> txs=new ArrayList<>(); LinearLayout page;
  static class Tx { String type,desc,date; long amount; boolean paid; Tx(String t,String d,long a,String dt,boolean p){type=t;desc=d;amount=a;date=dt;paid=p;} String month(){return date.substring(0,7);} }
  @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("ledger",0);load();render();}
  int[] jalali(int gy,int gm,int gd){
    int[] md={0,31,((gy%4==0&&gy%100!=0)||gy%400==0)?29:28,31,30,31,30,31,31,30,31,30,31};
    int y=gy-1600,m=gm-1,d=gd-1;
    long g=365L*y+(y+3)/4-(y+99)/100+(y+399)/400;
    for(int i=0;i<m;i++)g+=md[i]; g+=d; long j=g-79, n=j/12053; j%=12053;
    int jy=(int)(979+33*n+4*(j/1461)); j%=1461;
    if(j>=366){jy+=(int)((j-366)/365);j=(j-366)%365;}
    int jm,jd;if(j<186){jm=1+(int)(j/31);jd=1+(int)(j%31);}else{jm=7+(int)((j-186)/30);jd=1+(int)((j-186)%30);}
    return new int[]{jy,jm,jd};
  }
  String digits(String s){String a="0123456789",b="۰۱۲۳۴۵۶۷۸۹";for(int i=0;i<10;i++)s=s.replace(a.charAt(i),b.charAt(i));return s;}
  String today(){Calendar c=Calendar.getInstance();int[] j=jalali(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));return String.format(Locale.US,"%04d/%02d/%02d",j[0],j[1],j[2]);}
  String month(){return today().substring(0,7);}
  String money(long n){return digits(String.format(Locale.US,"%,d",n));}
  TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  GradientDrawable shape(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(24);if(stroke!=0)d.setStroke(2,stroke);return d;}
  TextView btn(String s,int color,Runnable r){TextView t=txt(s,15,color,true);t.setGravity(Gravity.CENTER);t.setPadding(8,15,8,15);t.setBackground(shape(Color.WHITE,color));t.setOnClickListener(v->r.run());return t;}
  void render(){
    ScrollView sc=new ScrollView(this);page=col();page.setPadding(16,18,16,18);page.setBackgroundColor(Color.rgb(247,249,255));sc.addView(page);
    TextView head=txt("حساب‌یار",27,Color.WHITE,true);head.setPadding(18,18,18,18);head.setBackground(shape(blue,0));page.addView(head);
    TextView mt=txt("گزارش ماه شمسی: "+digits(month()),16,ink,true);mt.setPadding(8,18,8,12);page.addView(mt);
    long in=0,out=0,debt=0;int active=0;
    for(Tx x:txs)if(x.month().equals(month())){if(x.type.equals("in"))in+=x.amount;else if(x.type.equals("out"))out+=x.amount;else if(x.type.equals("debt")&&!x.paid){debt+=x.amount;active++;}}
    LinearLayout cards=row();cards.addView(card("جمع دریافتی",in,green),new LinearLayout.LayoutParams(0,-2,1));cards.addView(card("جمع پرداختی",out,red),new LinearLayout.LayoutParams(0,-2,1));cards.addView(card("جمع بدهی",debt,purple),new LinearLayout.LayoutParams(0,-2,1));page.addView(cards);
    TextView bal=txt("مانده حساب: "+money(in-out)+" تومان\nبدهی فعال: "+digits(""+active),15,ink,true);bal.setPadding(8,18,8,18);page.addView(bal);
    TextView label=txt("ثبت اطلاعات",20,ink,true);label.setPadding(8,12,8,10);page.addView(label);
    LinearLayout a=row();a.addView(btn("＋ ثبت دریافت",green,()->form("in")),new LinearLayout.LayoutParams(0,-2,1));a.addView(btn("− ثبت پرداخت",red,()->form("out")),new LinearLayout.LayoutParams(0,-2,1));page.addView(a);
    LinearLayout b=row();b.addView(btn("ثبت بدهی",purple,()->form("debt")),new LinearLayout.LayoutParams(0,-2,1));b.addView(btn("گزارش ماهانه",blue,()->report()),new LinearLayout.LayoutParams(0,-2,1));page.addView(b);
    TextView dl=txt("بدهی‌های تسویه‌نشده",20,ink,true);dl.setPadding(8,22,8,10);page.addView(dl);boolean any=false;
    for(Tx x:new ArrayList<>(txs))if(x.type.equals("debt")&&!x.paid){any=true;LinearLayout item=col();item.setPadding(12,12,12,12);item.setBackground(shape(Color.WHITE,0));item.addView(txt(x.desc,16,ink,true));item.addView(txt(money(x.amount)+" تومان • "+digits(x.date),14,Color.GRAY,false));item.addView(btn("پرداخت شد؛ انتقال به پرداختی‌ها",green,()->settle(x)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=10;page.addView(item,lp);}
    if(!any)page.addView(txt("بدهی باز ثبت نشده است.",14,Color.GRAY,false));
    TextView trans=txt("پرداختی‌ها و دریافتی‌ها",20,ink,true);trans.setPadding(8,20,8,10);page.addView(trans);
    for(int i=txs.size()-1;i>=0;i--){Tx x=txs.get(i);if(x.type.equals("debt")&&!x.paid)continue;LinearLayout item=row();item.setPadding(12,12,12,12);item.setBackground(shape(Color.WHITE,0));LinearLayout detail=col();detail.addView(txt(x.desc,16,ink,true));detail.addView(txt(digits(x.date)+" • "+(x.type.equals("in")?"دریافت":"پرداخت"),13,Color.GRAY,false));item.addView(detail,new LinearLayout.LayoutParams(0,-2,1));item.addView(txt((x.type.equals("in")?"+":"−")+money(x.amount),15,x.type.equals("in")?green:red,true));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;page.addView(item,lp);}
    setContentView(sc);
  }
  View card(String name,long amount,int color){LinearLayout l=col();l.setGravity(Gravity.CENTER);l.setPadding(3,14,3,14);l.setBackground(shape(Color.WHITE,color));TextView a=txt(name,12,color,true);a.setGravity(Gravity.CENTER);TextView b=txt(money(amount),14,color,true);b.setGravity(Gravity.CENTER);TextView c=txt("تومان",11,color,false);c.setGravity(Gravity.CENTER);l.addView(a);l.addView(b);l.addView(c);return l;}
  void form(String type){LinearLayout l=col();EditText d=new EditText(this);d.setSingleLine(true);d.setHint("شرح");l.addView(d);EditText a=new EditText(this);a.setInputType(2);a.setHint("مبلغ به تومان");l.addView(a);new AlertDialog.Builder(this).setTitle(type.equals("in")?"ثبت دریافت":type.equals("out")?"ثبت پرداخت":"ثبت بدهی").setView(l).setNegativeButton("انصراف",null).setPositiveButton("ثبت",(di,w)->{try{String desc=d.getText().toString().trim();long amount=Long.parseLong(a.getText().toString().trim());if(desc.isEmpty()||amount<=0){toast("شرح و مبلغ معتبر وارد کنید");return;}txs.add(new Tx(type,desc,amount,today(),false));save();render();}catch(Exception e){toast("مبلغ را درست وارد کنید");}}).show();}
  void settle(Tx x){new AlertDialog.Builder(this).setTitle("تسویه بدهی").setMessage("مبلغ "+money(x.amount)+" تومان به پرداختی‌ها منتقل شود؟").setNegativeButton("لغو",null).setPositiveButton("تأیید",(d,w)->{x.paid=true;txs.add(new Tx("out","تسویه بدهی: "+x.desc,x.amount,today(),true));save();render();}).show();}
  void report(){long in=0,out=0,debt=0;for(Tx x:txs)if(x.month().equals(month())){if(x.type.equals("in"))in+=x.amount;else if(x.type.equals("out"))out+=x.amount;else if(x.type.equals("debt")&&!x.paid)debt+=x.amount;}new AlertDialog.Builder(this).setTitle("گزارش ماه شمسی "+digits(month())).setMessage("جمع دریافتی: "+money(in)+" تومان\nجمع پرداختی: "+money(out)+" تومان\nمانده: "+money(in-out)+" تومان\nبدهی باقی‌مانده: "+money(debt)+" تومان").setPositiveButton("بستن",null).show();}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  void save(){JSONArray a=new JSONArray();try{for(Tx x:txs){JSONObject o=new JSONObject();o.put("type",x.type);o.put("desc",x.desc);o.put("amount",x.amount);o.put("date",x.date);o.put("paid",x.paid);a.put(o);}prefs.edit().putString("txs",a.toString()).apply();}catch(Exception ignored){}}
  void load(){try{JSONArray a=new JSONArray(prefs.getString("txs","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);txs.add(new Tx(o.getString("type"),o.getString("desc"),o.getLong("amount"),o.getString("date"),o.getBoolean("paid")));}}catch(Exception ignored){}}
}