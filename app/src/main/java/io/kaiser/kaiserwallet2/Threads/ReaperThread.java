package io.kaiser.kaiserwallet2.Threads;

import android.util.Log;

public class ReaperThread extends Thread implements Runnable{

    private final long MAX_KILL_TIME = 1000 * 3 * 60; //밀리초 뒤 종료
//    private final long MAX_KILL_TIME = 1000 * 3; //밀리초 뒤 종료

    private final long TIME_GAP = 20; //밀리초 간격의 시간

    private long remainLifeTime = Long.MAX_VALUE; //남은 인생 시간

    private boolean refreshLifeTime = true;

    //저승사자
    private static ReaperThread reaper = null;

    public static ReaperThread getInstance(){
        if (reaper == null){
            synchronized (ReaperThread.class){
                if (reaper == null){
                    reaper = new ReaperThread();
                }
            }
        }
        return reaper;
    }


    @Override
    public void run() {
        super.run();

        while(!itsTimeToDie())
        {
            sufferingNightmares();

            refresh();

        }

        meetHim();
    }

    public ReaperThread()
    {
        Log.d("TTTT","born");
        this.start();
    }


    //잠 제울때
    public void makeMeSleep()
    {
        Log.d("TTTT","makeMeSleep");

        synchronized (ReaperThread.class){
            refreshLifeTime = false;
            remainLifeTime = MAX_KILL_TIME;
        //    mutex.unlock();
        }
    }

    //잠에서 깨울때
    public void wakeMeUpFromSleep()
    {
        Log.d("TTTT","wakeMeUpFromSleep");


        synchronized (ReaperThread.class){
            refreshLifeTime = true;
            remainLifeTime = Integer.MAX_VALUE;
            //mutex.lock();
        }
    }

    private void refresh()
    {
        synchronized (ReaperThread.class){
            if(refreshLifeTime)
            {
                remainLifeTime = Integer.MAX_VALUE;
            }
        }
    }

    //생명시간 단축
    private void sufferingNightmares()
    {
        //Log.d("TTTT",String.format("remainLifeTime:%d",remainLifeTime));
        synchronized (ReaperThread.class) {
            remainLifeTime -= TIME_GAP;
        }

        //악몽을 꿀 시간이야
        try {
            Thread.sleep(TIME_GAP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //남은 시간 체크
    private boolean itsTimeToDie()
    {
        boolean ret = false;

        synchronized (ReaperThread.class){
            if(remainLifeTime <= 0)
            {
                ret = true;
            }
        }

        return ret;
    }

    //죽음
    private void meetHim()
    {
        Log.d("TTTT","meetHim");

        System.exit(0);
    }

}
