let setupPromise: Promise<void> | null = null;

export const ensureMonacoSetup = async (): Promise<void> => {
  if (!setupPromise) {
    setupPromise = import('./userWorker').then(() => undefined);
  }
  await setupPromise;
};
