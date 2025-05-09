package com.ejercicio_2.ejercicio_2;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadsAndConcurrency {

    private static final int NUM_TASKS = 10_000;
    private static final int TASK_DURATION_MS = 200;

    // Mock task that simulates an I/O operation
    static class SimulatedTask implements Runnable {
        private final int taskId;

        public SimulatedTask(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(Duration.ofMillis(TASK_DURATION_MS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Tarea " + taskId + " interrumpida.");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Simulando " + NUM_TASKS + " tareas de I/O (" + TASK_DURATION_MS + "ms cada una).\n");

        executeWithClassicExecutor();
        System.out.println("\n-----\n");
        executeWithVirtualThreads();

        System.out.println("\nSimulación completada.");
    }

    public static void executeWithClassicExecutor() throws InterruptedException {
        System.out.println("Iniciando prueba con ExecutorService clásico...");
        int poolSize = 100;
        System.out.println("Tamaño del pool de hilos de plataforma: " + poolSize);

        int activeThreadsBefore = Thread.activeCount();
        System.out.println("Hilos activos (aproximadamente) antes de iniciar el pool fijo: " + activeThreadsBefore);


        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < NUM_TASKS; i++) {
            tasks.add(new SimulatedTask(i));
        }

        Instant startTime = Instant.now();

        AtomicInteger peakThreadsInPool = new AtomicInteger(0);
        Thread threadMonitor = new Thread(() -> {
            while (!executor.isTerminated()) {
                try {
                    int currentActive = Thread.activeCount();
                    if (currentActive > peakThreadsInPool.get()) {
                        peakThreadsInPool.set(currentActive);
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        for (Runnable task : tasks) {
            executor.submit(task);
        }

        Thread.sleep(500);
        int activeThreadsDuring = Thread.activeCount();
        System.out.println("Hilos activos (aproximadamente) durante la ejecución (pool fijo): " + activeThreadsDuring);
        System.out.println("Se espera un incremento cercano a " + poolSize + " si el pool se llena");


        System.out.println("Todas las tareas enviadas al pool fijo. Esperando finalización...");
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.MINUTES);

        Instant endTime = Instant.now();
        long totalTimeMs = Duration.between(startTime, endTime).toMillis();

        if (!terminated) {
            System.err.println("ALERTA: El pool fijo no terminó en el tiempo esperado.");
            executor.shutdownNow();
        }

        System.out.println("Tiempo total de ejecución (FixedThreadPool): " + totalTimeMs + " ms");
        Thread.sleep(500);
        int activeThreadsAfter = Thread.activeCount();

        System.out.println("Hilos activos (aproximadamente) después de terminar (pool fijo): " + activeThreadsAfter);
    }


    public static void executeWithVirtualThreads() throws InterruptedException {
        System.out.println("Iniciando prueba con Hilos Virtuales (newVirtualThreadPerTaskExecutor)...");

        int activeThreadsBefore = Thread.activeCount();

        System.out.println("Hilos activos (aproximadamente) antes de iniciar con hilos virtuales: " + activeThreadsBefore);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < NUM_TASKS; i++) {
            tasks.add(new SimulatedTask(i));
        }

        Instant startTime = Instant.now();
        AtomicInteger peakPlatformThreads = new AtomicInteger(activeThreadsBefore);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Thread monitor = new Thread(() -> {
                try {
                    while (!executor.isTerminated()) {
                        int currentActiveThreads = Thread.activeCount();
                        peakPlatformThreads.updateAndGet(peak -> Math.max(peak, currentActiveThreads));
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            monitor.setDaemon(true);
            monitor.start();

            for (Runnable task : tasks) {
                executor.submit(task);
            }

            executor.shutdown();
            boolean terminated = executor.awaitTermination(10, TimeUnit.MINUTES);

            if (!terminated) {
                System.err.println("ALERTA: El ejecutor de hilos virtuales no terminó en el tiempo esperado.");
                executor.shutdownNow();
            }

            monitor.interrupt();
            monitor.join(500);
        }

        Instant endTime = Instant.now();
        long totalTimeMs = Duration.between(startTime, endTime).toMillis();

        System.out.println("Tiempo total de ejecución (Hilos Virtuales): " + totalTimeMs + " ms");
        System.out.println("Pico de hilos de plataforma activos (aproximadamente) durante ejecución virtual: " + peakPlatformThreads.get());

        Thread.sleep(500);
        int activeThreadsAfter = Thread.activeCount();

        System.out.println("Hilos activos (aproximadamente) después de terminar (hilos virtuales): " + activeThreadsAfter);
    }
}