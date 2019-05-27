package me.mircea.riw.crawler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ActiveDownloadQueue<E> extends LinkedBlockingQueue<E> {
    private Set<E> visited;

    public ActiveDownloadQueue() {
        super();
        this.visited = new HashSet<>();
    }

    @Override
    public boolean add(E e) {
        if (visited.contains(e)) {
            return false;
        } else {
            visited.add(e);
            return super.add(e);
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E foundElement = super.poll(timeout, unit);
        removeFromMemory(foundElement);
        return foundElement;
    }

    @Override
    public E poll() {
        E foundElement =  super.poll();
        removeFromMemory(foundElement);
        return foundElement;
    }

    private void removeFromMemory(E e) {
        if (e != null)
            visited.remove(e);
    }
}
