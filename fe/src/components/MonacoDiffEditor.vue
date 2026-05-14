<template>
  <div ref="container" style="width:100%;height:400px;"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import type * as Monaco from 'monaco-editor';
import { ensureMonacoSetup } from '@/assets/ts/monacoSetup';

const props = defineProps<{
  original: string;
  modified: string;
  language?: string;
}>();

const container = ref<HTMLDivElement | null>(null);
let editor: Monaco.editor.IStandaloneDiffEditor | null = null;
let monacoInstance: typeof import('monaco-editor') | null = null;

onMounted(async () => {
  if (container.value) {
    await ensureMonacoSetup();
    const monaco = await import('monaco-editor');
    monacoInstance = monaco;
    editor = monaco.editor.createDiffEditor(container.value, {
      automaticLayout: true,
      readOnly: true,
      theme: 'vs-dark',
      useInlineViewWhenSpaceIsLimited: false,
    });
    const originalModel = monaco.editor.createModel(props.original, props.language || 'python');
    const modifiedModel = monaco.editor.createModel(props.modified, props.language || 'python');
    editor.setModel({ original: originalModel, modified: modifiedModel });
  }
});

watch(() => [props.original, props.modified], ([newOriginal, newModified]) => {
  if (editor) {
    const originalModel = editor.getModel()?.original;
    const modifiedModel = editor.getModel()?.modified;
    if (originalModel && modifiedModel) {
      originalModel.setValue(newOriginal!);
      modifiedModel.setValue(newModified!);
    }
  }
});

onBeforeUnmount(() => {
  if (editor) {
    editor.dispose();
    editor = null;
  }
  monacoInstance = null;
});
</script>