package cn.garymb.ygomobile.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.base.bj.paysdk.utils.TrPay;
import com.google.android.material.navigation.NavigationView;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.tencent.bugly.beta.Beta;
import com.tencent.smtt.sdk.QbSdk;
import com.tubb.smrv.SwipeMenuRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import cn.garymb.ygodata.YGOGameOptions;
import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.YGOMobileActivity;
import cn.garymb.ygomobile.YGOStarter;
import cn.garymb.ygomobile.bean.Deck;
import cn.garymb.ygomobile.bean.ServerInfo;
import cn.garymb.ygomobile.bean.ServerList;
import cn.garymb.ygomobile.bean.events.ServerInfoEvent;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.activities.BaseActivity;
import cn.garymb.ygomobile.ui.activities.FileLogActivity;
import cn.garymb.ygomobile.ui.activities.WebActivity;
import cn.garymb.ygomobile.ui.adapters.ServerListAdapter;
import cn.garymb.ygomobile.ui.adapters.SimpleListAdapter;
import cn.garymb.ygomobile.ui.cards.CardSearchAcitivity;
import cn.garymb.ygomobile.ui.cards.DeckManagerActivity;
import cn.garymb.ygomobile.ui.cards.deck.DeckUtils;
import cn.garymb.ygomobile.ui.mycard.MyCardActivity;
import cn.garymb.ygomobile.ui.plus.DefaultOnBoomListener;
import cn.garymb.ygomobile.ui.plus.DialogPlus;
import cn.garymb.ygomobile.ui.plus.VUiKit;
import cn.garymb.ygomobile.ui.preference.SettingsActivity;
import cn.garymb.ygomobile.ui.widget.Shimmer;
import cn.garymb.ygomobile.ui.widget.ShimmerTextView;
import cn.garymb.ygomobile.utils.ComponentUtils;
import cn.garymb.ygomobile.utils.FileLogUtil;
import cn.garymb.ygomobile.utils.PayUtils;
import cn.garymb.ygomobile.utils.ScreenUtil;
import cn.garymb.ygomobile.utils.YGOUtil;

import static cn.garymb.ygomobile.Constants.ASSET_SERVER_LIST;

public abstract class HomeActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    //卡查关键字
    public static final String[] cardSearchKey = new String[]{"?", "？"};
    //加房关键字
    public static final String[] passwordPrefix = {
            "M,", "m,", "T,", "PR,", "pr,", "AI,", "ai,", "LF2,", "lf2,", "M#", "m#", "T#", "t#",
            "PR#", "pr#", "NS#", "ns#", "S#", "s#", "AI#", "ai#", "LF2#", "lf2#", "R#", "r#"
    };
    //卡组复制
    public static final String[] DeckTextKey = new String[]{"#main"};
    /***
     * 剪贴板监听复制内容
     */
    private final static String DECK_URL_PREFIX = Constants.SCHEME_APP + "://" + Constants.URI_HOST;
    //卡查内容
    public static String cardSearchMessage = "";
    public static String DeckText = "";
    protected SwipeMenuRecyclerView mServerList;
    long exitLasttime = 0;
    ShimmerTextView tv;
    Shimmer shimmer;
    private ServerListAdapter mServerListAdapter;
    private ServerListManager mServerListManager;
    private ClipboardManager cm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setExitAnimEnable(false);
        mServerList = $(R.id.list_server);
        mServerListAdapter = new ServerListAdapter(this);
        //server list
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mServerList.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mServerList.addItemDecoration(dividerItemDecoration);
        mServerList.setAdapter(mServerListAdapter);
        mServerListManager = new ServerListManager(this, mServerListAdapter);
        mServerListManager.bind(mServerList);
        mServerListManager.syncLoadData();
        //event
        EventBus.getDefault().register(this);
        initBoomMenuButton($(R.id.bmb));
        AnimationShake();
        tv = (ShimmerTextView) findViewById(R.id.shimmer_tv);
        toggleAnimation(tv);

        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                if (arg0) {
                    //  Toast.makeText(getActivity(), "加载成功", Toast.LENGTH_LONG).show();
                } else {
                    //Toast.makeText(getActivity(), "部分资源因机型原因加载错误，不影响使用", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCoreInitFinished() {
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(this, cb);
        //trpay
        //TrPay.getInstance(HomeActivity.this).initPaySdk("e1014da420ea4405898c01273d6731b6", "YGOMobile");
        //check update
        //Beta.checkUpgrade(false, false);
        //DuelAssistantService
        YGOUtil.startDuelService(this);
        //萌卡
        StartMycard();
        checkNotch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BacktoDuel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AppsSettings.get().isServiceDuelAssistant() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getClipboard();
                }
            }, 500);
        }
    }

    //检查是否有刘海
    private void checkNotch() {
        ScreenUtil.findNotchInformation(HomeActivity.this, new ScreenUtil.FindNotchInformation() {
            @Override
            public void onNotchInformation(boolean isNotch, int notchHeight, int phoneType) {
                try {
                    FileLogUtil.writeAndTime("检查刘海" + isNotch + "   " + notchHeight);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                AppsSettings.get().setNotchHeight(notchHeight);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerInfoEvent(ServerInfoEvent event) {
        if (event.delete) {
            DialogPlus dialogPlus = new DialogPlus(getContext());
            dialogPlus.setTitle(R.string.question);
            dialogPlus.setMessage(R.string.delete_server_info);
            dialogPlus.setMessageGravity(Gravity.CENTER_HORIZONTAL);
            dialogPlus.setLeftButtonListener((dialog, which) -> {
                mServerListManager.delete(event.position);
                mServerListAdapter.notifyDataSetChanged();
                dialog.dismiss();
            });
            dialogPlus.setCancelable(true);
            dialogPlus.setOnCloseLinster(null);
            dialogPlus.show();
        } else if (event.join) {
            joinRoom(event.position);
        } else {
            mServerListManager.showEditDialog(event.position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (doMenu(item.getItemId())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (doMenu(item.getItemId())) {
            return true;
        }
        return false;
    }

    @Override
    public HomeActivity getActivity() {
        return this;
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private boolean doMenu(int id) {
        switch (id) {
            case R.id.nav_donation: {

                final DialogPlus dialog = new DialogPlus(getContext());
                dialog.setContentView(R.layout.dialog_alipay_or_wechat);
                dialog.setTitle(R.string.logo_text);
                dialog.show();
                View viewDialog = dialog.getContentView();
                Button btnAlipay = viewDialog.findViewById(R.id.button_alipay);
                Button btnTrpay = viewDialog.findViewById(R.id.button_trpay);
                Button btnpaypal = viewDialog.findViewById(R.id.button_paypal);
                btnAlipay.setOnClickListener((v) -> {
                    PayUtils.openAlipayPayPage(getContext(), Constants.ALIPAY_URL);
                    dialog.dismiss();
                });
                btnTrpay.setOnClickListener((v) -> {
                    WebActivity.open(this, getString(R.string.donation), Constants.AFDIAN_URL);
                    dialog.dismiss();
                });
                btnpaypal.setOnClickListener((v) -> {
                    WebActivity.open(this, getString(R.string.donation), Constants.PAYPAL_URL);
                    dialog.dismiss();
                });
            }
            break;
            case R.id.action_game:
                openGame();
                break;
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.action_quit: {
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                DialogPlus builder = new DialogPlus(this);
                builder.setTitle(R.string.question);
                builder.setMessage(R.string.quit_tip);
                builder.setMessageGravity(Gravity.CENTER_HORIZONTAL);
                builder.setLeftButtonListener((dlg, s) -> {
                    dlg.dismiss();
                    finish();
                });
                builder.show();
            }
            break;
            case R.id.action_add_server:
                mServerListManager.addServer();
                break;
            case R.id.action_card_search:
                startActivity(new Intent(this, CardSearchAcitivity.class));
                break;
            case R.id.action_deck_manager:
                startActivity(new Intent(this, DeckManagerActivity.getDeckManager()));
                break;
            case R.id.action_join_qq_group:
                String key = "LqrY0AY-8NoMiAaRDymk5jIxFyU_MFSp";
                joinQQGroup(key);
                break;
            case R.id.action_help: {
                final DialogPlus dialog = new DialogPlus(getContext());
                dialog.setContentView(R.layout.dialog_help);
                dialog.setTitle(R.string.question);
                dialog.show();
                View viewDialog = dialog.getContentView();
                Button btnMasterRule = viewDialog.findViewById(R.id.masterrule);
                Button btnTutorial = viewDialog.findViewById(R.id.tutorial);

                btnMasterRule.setOnClickListener((v) -> {
                    WebActivity.open(this, getString(R.string.masterrule), Constants.URL_MASTERRULE_CN);
                    dialog.dismiss();
                });
                btnTutorial.setOnClickListener((v) -> {
                    WebActivity.open(this, getString(R.string.help), Constants.URL_HELP);
                    dialog.dismiss();
                });

            }
            break;
            case R.id.action_reset_game_res:
                updateImages();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - exitLasttime <= 3000) {
            super.onBackPressed();
        } else {
            showToast(R.string.back_tip, Toast.LENGTH_SHORT);
            exitLasttime = System.currentTimeMillis();
        }
    }

    public void joinRoom(int position) {
        ServerInfo serverInfo = mServerListAdapter.getItem(position);
        if (serverInfo == null) {
            return;
        }
        //进入房间
        DialogPlus builder = new DialogPlus(getContext());
        builder.setTitle(R.string.intput_room_name);
        builder.setContentView(R.layout.dialog_room_name);
        EditText editText = builder.bind(R.id.room_name);
        ListView listView = builder.bind(R.id.room_list);
        SimpleListAdapter simpleListAdapter = new SimpleListAdapter(getContext());
        simpleListAdapter.set(AppsSettings.get().getLastRoomList());
        listView.setAdapter(simpleListAdapter);
        listView.setOnItemClickListener((a, v, pos, index) -> {
//                builder.dismiss();
            String name = simpleListAdapter.getItemById(index);
            editText.setText(name);
//                joinGame(serverInfo, name);
        });
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                builder.dismiss();
                String name = editText.getText().toString();
                if (!TextUtils.isEmpty(name)) {
                    List<String> items = simpleListAdapter.getItems();
                    int index = items.indexOf(name);
                    if (index >= 0) {
                        items.remove(index);
                        items.add(0, name);
                    } else {
                        items.add(0, name);
                    }
                    AppsSettings.get().setLastRoomList(items);
                    simpleListAdapter.notifyDataSetChanged();
                }
                joinGame(serverInfo, name);
                return true;
            }
            return false;
        });
        listView.setOnItemLongClickListener((a, v, i, index) -> {
            String name = simpleListAdapter.getItemById(index);
            int pos = simpleListAdapter.findItem(name);
            if (pos >= 0) {
                simpleListAdapter.remove(pos);
                simpleListAdapter.notifyDataSetChanged();
                AppsSettings.get().setLastRoomList(simpleListAdapter.getItems());
            }
            return true;
        });
        builder.setLeftButtonText(R.string.join_game);
        builder.setLeftButtonListener((dlg, i) -> {
            dlg.dismiss();
            //保存名字
            String name = editText.getText().toString();
            if (!TextUtils.isEmpty(name)) {
                List<String> items = simpleListAdapter.getItems();
                int index = items.indexOf(name);
                if (index >= 0) {
                    items.remove(index);
                    items.add(0, name);
                } else {
                    items.add(0, name);
                }
                AppsSettings.get().setLastRoomList(items);
                simpleListAdapter.notifyDataSetChanged();
            }
            joinGame(serverInfo, name);
        });
        builder.setOnCloseLinster((dlg) -> {
            dlg.dismiss();
        });
        builder.setOnCancelListener((dlg) -> {
        });
        builder.show();
    }

    void joinGame(ServerInfo serverInfo, String name) {
        YGOGameOptions options = new YGOGameOptions();
        options.mServerAddr = serverInfo.getServerAddr();
        options.mUserName = serverInfo.getPlayerName();
        options.mPort = serverInfo.getPort();
        options.mRoomName = name;
        YGOStarter.startGame(this, options);
    }

    protected abstract void checkResourceDownload(ResCheckTask.ResCheckListener listener);

    protected abstract void openGame();

    public abstract void updateImages();

    private void initBoomMenuButton(BoomMenuButton menu) {
        final SparseArray<Integer> mMenuIds = new SparseArray<>();
        addMenuButton(mMenuIds, menu, R.id.action_join_qq_group, R.string.Join_QQ, R.drawable.joinqqgroup);
        addMenuButton(mMenuIds, menu, R.id.action_card_search, R.string.card_search, R.drawable.search);
        addMenuButton(mMenuIds, menu, R.id.action_deck_manager, R.string.deck_manager, R.drawable.deck);

        addMenuButton(mMenuIds, menu, R.id.action_add_server, R.string.action_add_server, R.drawable.addsever);
        addMenuButton(mMenuIds, menu, R.id.action_game, R.string.action_game, R.drawable.start);
        addMenuButton(mMenuIds, menu, R.id.action_help, R.string.help, R.drawable.help);

        addMenuButton(mMenuIds, menu, R.id.action_reset_game_res, R.string.reset_game_res, R.drawable.downloadimages);
        addMenuButton(mMenuIds, menu, R.id.action_settings, R.string.settings, R.drawable.setting);
        addMenuButton(mMenuIds, menu, R.id.nav_donation, R.string.donation, R.drawable.about);

        //设置展开或隐藏的延时。 默认值为 800ms。
        menu.setDuration(100);
        //设置每两个子按钮之间动画的延时（ms为单位）。 比如，如果延时设为0，那么所有子按钮都会同时展开或隐藏，默认值为100ms。
        menu.setDelay(20);

        menu.setOnBoomListener(new DefaultOnBoomListener() {
            @Override
            public void onClicked(int index, BoomButton boomButton) {
                doMenu(mMenuIds.get(index));
            }
        });

    }

    private void addMenuButton(SparseArray<Integer> mMenuIds, BoomMenuButton menuButton, int menuId, int stringId, int image) {
        addMenuButton(mMenuIds, menuButton, menuId, getString(stringId), image);
    }

    private void addMenuButton(SparseArray<Integer> mMenuIds, BoomMenuButton menuButton, int menuId, String str, int image) {
        TextOutsideCircleButton.Builder builder = new TextOutsideCircleButton.Builder()
                .shadowColor(Color.TRANSPARENT)
                .normalColor(Color.TRANSPARENT)
                .normalImageRes(image)
                .normalText(str);
        menuButton.addBuilder(builder);
        mMenuIds.put(mMenuIds.size(), menuId);
    }

    public void AnimationShake() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);//加载动画资源文件
        findViewById(R.id.cube).startAnimation(shake); //给组件播放动画效果
    }

    public void toggleAnimation(View target) {
        if (shimmer != null && shimmer.isAnimating()) {
            shimmer.cancel();
        } else {
            shimmer = new Shimmer();
            shimmer.start(tv);
        }
    }

    public void StartMycard() {
        ImageView iv_mc = $(R.id.btn_mycard);
        iv_mc.setOnClickListener((v) -> {
            if (Constants.SHOW_MYCARD) {
                startActivity(new Intent(this, MyCardActivity.class));
            }
        });
        iv_mc.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(HomeActivity.this, FileLogActivity.class));
                return true;
            }
        });
    }

    public void BacktoDuel() {
        tv.setOnClickListener((v) -> {
            openGame();
        });
        if (ComponentUtils.isActivityRunning(this, new ComponentName(this, YGOMobileActivity.class))) {
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    public boolean joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
            return false;
        }
    }

    public void getClipboard() {
        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = cm.getPrimaryClip();
        if (clipData == null)
            return;
        ClipData.Item cs = clipData.getItemAt(0);
        final String clipMessage;
        if (cs.getText() != null) {
            clipMessage = cs.getText().toString();
        } else {
            clipMessage = null;
        }
        //如果复制的内容为空则不执行下面的代码
        if (TextUtils.isEmpty(clipMessage)) {
            return;
        }
        clipData = ClipData.newPlainText("", "");
        cm.setPrimaryClip(clipData);
        //如果复制的内容是多行作为卡组去判断
        if (clipMessage.contains("\n")) {
            for (String s : DeckTextKey) {
                //只要包含其中一个关键字就视为卡组
                if (clipMessage.contains(s)) {
                    saveDeck(clipMessage, false);
                    return;
                }
            }
            return;
        }
        //如果是卡组url
        int deckStart = clipMessage.indexOf(DECK_URL_PREFIX);
        if (deckStart != -1) {
            saveDeck(clipMessage.substring(deckStart + DECK_URL_PREFIX.length(), clipMessage.length()), true);
            return;
        }

        int start = -1;
        int end = -1;
        String passwordPrefixKey = null;
        for (String s : passwordPrefix) {
            start = clipMessage.indexOf(s);
            passwordPrefixKey = s;
            if (start != -1) {
                break;
            }
        }
        if (start != -1) {
            //如果密码含有空格，则以空格结尾
            end = clipMessage.indexOf(" ", start);
            //如果不含有空格则取片尾所有
            if (end == -1) {
                end = clipMessage.length();
            } else {
                //如果只有密码前缀而没有密码内容则不跳转
                if (end - start == passwordPrefixKey.length())
                    return;
            }
            QuickjoinRoom(clipMessage, start, end);
        } else {
            for (String s : cardSearchKey) {
                int cardSearchStart = clipMessage.indexOf(s);
                if (cardSearchStart != -1) {
                    //卡查内容
                    cardSearchMessage = clipMessage.substring(cardSearchStart + s.length(), clipMessage.length());
                    //如果复制的文本里带？号后面没有内容则不跳转
                    if (TextUtils.isEmpty(cardSearchMessage)) {
                        return;
                    }
                    //如果卡查内容包含“=”并且复制的内容包含“.”不卡查
                    if (cardSearchMessage.contains("=") && clipMessage.contains(".")) {
                        return;
                    }
                    Intent intent = new Intent(this, CardSearchAcitivity.class);
                    intent.putExtra(CardSearchAcitivity.SEARCH_MESSAGE, cardSearchMessage);
                    startActivity(intent);
                }
            }
        }
    }

    private void saveDeck(String deckMessage, boolean isUrl) {
        DialogPlus dialog = new DialogPlus(this);
        dialog.setTitle(R.string.question);
        dialog.setMessage(R.string.find_deck_text);
        dialog.setMessageGravity(Gravity.CENTER_HORIZONTAL);
        dialog.setLeftButtonText(R.string.Cancel);
        dialog.setRightButtonText(R.string.save_n_open);
        dialog.show();
        dialog.setLeftButtonListener((dlg, s) -> {
            dialog.dismiss();
        });
        dialog.setRightButtonListener((dlg, s) -> {
            dialog.dismiss();
            //如果是卡组url
            if (isUrl) {
                Deck deckInfo = new Deck(getString(R.string.rename_deck) + System.currentTimeMillis(), Uri.parse(deckMessage));
                File file = deckInfo.saveTemp(AppsSettings.get().getDeckDir());
                Intent startdeck = new Intent(this, DeckManagerActivity.getDeckManager());
                startdeck.putExtra(Intent.EXTRA_TEXT, file.getAbsolutePath());
                startActivity(startdeck);
            } else {
                //如果是卡组文本
                try {
                    //以当前时间戳作为卡组名保存卡组
                    File file = DeckUtils.save(getString(R.string.rename_deck) + System.currentTimeMillis(), deckMessage);
                    Intent startdeck = new Intent(this, DeckManagerActivity.getDeckManager());
                    startdeck.putExtra(Intent.EXTRA_TEXT, file.getAbsolutePath());
                    startActivity(startdeck);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.save_failed_bcos) + e, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void QuickjoinRoom(String ss, int start, int end) {
        final String password = ss.substring(start, ss.length());
        DialogPlus dialog = new DialogPlus(this);
        dialog.setTitle(R.string.question);
        dialog.setMessage(getString(R.string.quick_join) + password + "\"");
        dialog.setMessageGravity(Gravity.CENTER_HORIZONTAL);
        dialog.setLeftButtonText(R.string.Cancel);
        dialog.setRightButtonText(R.string.join);
        dialog.show();
        dialog.setLeftButtonListener((dlg, s) -> {
            dialog.dismiss();
        });
        dialog.setRightButtonListener((dlg, s) -> {
            dialog.dismiss();
            ServerListAdapter mServerListAdapter = new ServerListAdapter(this);
            ServerListManager mServerListManager = new ServerListManager(this, mServerListAdapter);
            mServerListManager.syncLoadData();
            File xmlFile = new File(getFilesDir(), Constants.SERVER_FILE);
            VUiKit.defer().when(() -> {
                ServerList assetList = ServerListManager.readList(this.getAssets().open(ASSET_SERVER_LIST));
                ServerList fileList = xmlFile.exists() ? ServerListManager.readList(new FileInputStream(xmlFile)) : null;
                if (fileList == null) {
                    return assetList;
                }
                if (fileList.getVercode() < assetList.getVercode()) {
                    xmlFile.delete();
                    return assetList;
                }
                return fileList;
            }).done((list) -> {
                if (list != null) {
                    ServerInfo serverInfo = list.getServerInfoList().get(0);
                    joinGame(serverInfo, password);
                }
            });
        });
    }
}
