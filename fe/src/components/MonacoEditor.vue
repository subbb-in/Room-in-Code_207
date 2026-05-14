<template>
  <div ref="container" class="size-full"></div>
</template>

<script setup lang="ts">
import { inject, onMounted, onUnmounted, ref, watch, type Ref } from 'vue';
import { MonacoBinding } from 'y-monaco'
import type * as Monaco from 'monaco-editor';
import * as Y from 'yjs'
import { useMatchStore } from '@/stores/matchStore';
import { useUserStore } from '@/stores/userStore';
import { ensureMonacoSetup } from '@/assets/ts/monacoSetup';

const userStore = useUserStore();
const matchStore = useMatchStore();


const ydoc = inject<Ref<Y.Doc | null>>('ydoc', ref(null));
const container = ref<HTMLDivElement>();

let editor: Monaco.editor.IStandaloneCodeEditor | null = null;
let binding: MonacoBinding | null = null;
let monacoInstance: typeof import('monaco-editor') | null = null;

const resolveMonacoLanguage = (lang?: string | null) => {
  const value = (lang || 'python').toLowerCase();
  if (value === 'cpp' || value === 'c++') return 'cpp';
  if (value === 'java') return 'java';
  return 'python';
};

const getLanguageTemplate = (lang?: string | null) => {
  const value = (lang || 'python').toLowerCase();
  if (value === 'java') {
    return [
      'import java.io.*;',
      'import java.util.*;',
      '',
      'public class Main {',
      '    public static void main(String[] args) throws Exception {',
      '        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));',
      '        StringBuilder sb = new StringBuilder();',
      '        String line;',
      '        while ((line = br.readLine()) != null) {',
      '            sb.append(line);',
      '            sb.append("\n");',
      '        }',
      '        String input = sb.toString();',
      '        System.out.print(solution(input));',
      '    }',
      '',
      '    public static String solution(String input) {',
      '        // TODO: write your code here',
      '        return "";',
      '    }',
      '}',
      ''
    ].join('\n');
  }
  if (value === 'cpp' || value === 'c++') {
    return [
      '#include <bits/stdc++.h>',
      'using namespace std;',
      '',
      'string solution(const string& input) {',
      '    // TODO: write your code here',
      '    return "";',
      '}',
      '',
      'int main() {',
      '    ios::sync_with_stdio(false);',
      '    cin.tie(nullptr);',
      '',
      '    string input((istreambuf_iterator<char>(cin)), istreambuf_iterator<char>());',
      '    cout << solution(input);',
      '    return 0;',
      '}',
      ''
    ].join('\n');
  }
  return [
    'def solution(input):',
    '    # TODO: write your code here',
    '    return ""',
    '',
    'if __name__ == "__main__":',
    '    import sys',
    '    data = sys.stdin.read()',
    '    print(solution(data))',
    ''
  ].join('\n');
};

const normalizeTemplateText = (value: string) => value.replace(/\r\n/g, '\n').trim();

const isTemplateCode = (value: string) => {
  const normalized = normalizeTemplateText(value);
  if (!normalized) return false;

  const templates = [
    getLanguageTemplate('python'),
    getLanguageTemplate('java'),
    getLanguageTemplate('cpp')
  ];

  return templates.some((template) => normalizeTemplateText(template) === normalized);
};

const applyLanguageTemplate = (lang?: string | null) => {
  const templateEnabled = false;
  if (!templateEnabled) return;

  const doc = ydoc.value;
  if (!doc) return;

  const ytext = doc.getText('code');
  const currentCode = ytext.toString();
  const normalized = normalizeTemplateText(currentCode);
  const shouldReplace = normalized.length === 0 || isTemplateCode(currentCode);
  if (!shouldReplace) return;

  const template = getLanguageTemplate(lang);
  if (!template) return;

  ytext.delete(0, ytext.length);
  ytext.insert(0, template);
};

const updateReadOnly = () => {
  if (!editor) return;
  
  const currentTurnUserId = Number(matchStore.gameInfo.currentTurnUserId);
  const myUserId = userStore.user?.userId;
  const phaseType = matchStore.gameInfo.phaseType;
  
  const isMyTurn = currentTurnUserId === myUserId;
  const isDesignPhase = phaseType === 'DESIGN';
  const shouldBeReadOnly = isDesignPhase || !isMyTurn;
  
  editor.updateOptions({ readOnly: shouldBeReadOnly });
};

watch(() => matchStore.gameInfo.currentTurnUserId, updateReadOnly, { immediate: true });
watch(() => matchStore.gameInfo.phaseType, updateReadOnly, { immediate: true });

watch(
  () => matchStore.gameInfo.programmingLanguage,
  (lang) => {
    if (!editor || !monacoInstance) return;
    const model = editor.getModel();
    if (!model) return;
    const monacoLang = resolveMonacoLanguage(lang);
    monacoInstance.editor.setModelLanguage(model, monacoLang);
    applyLanguageTemplate(lang);
  },
  { immediate: true }
);

const bindYDoc = (doc: Y.Doc) => {
  if (!editor || binding) return;

  const model = editor.getModel();
  if (!model) return;

  // 기존 문서 내용을 모델에 반영
  const ytext = doc.getText('code');
  if (model.getValue() !== ytext.toString()) {
    model.setValue(ytext.toString());
  }

  console.info('[MonacoEditor: Yjs] 바인딩 생성', {
    clientId: doc.clientID,
    textLength: ytext.length,
    modelLength: model.getValueLength()
  });

  binding = new MonacoBinding(
    ytext,
    model,
    new Set([editor]),
    null
  );
};

// ydoc이 준비되면 binding 생성/재생성
watch(
  () => ydoc.value,
  (newYdoc, oldYdoc) => {
    if (!newYdoc || !editor) return;

    const changed = oldYdoc !== newYdoc;
    if (changed && binding) {
      console.info('[MonacoEditor: Yjs] ydoc 변경 감지 - binding 재생성', {
        prevClientId: oldYdoc?.clientID,
        nextClientId: newYdoc.clientID
      });
      binding.destroy();
      binding = null;
    }

    if (!binding) {
      console.info('[MonacoEditor: Yjs] ydoc 연결됨', {
        clientId: newYdoc.clientID,
        hasCode: newYdoc.share.has('code')
      });
      bindYDoc(newYdoc);
    }
  },
  { immediate: true }
);

watch(
  () => ydoc.value,
  (doc) => {
    if (!doc) return;
    console.info('[MonacoEditor: Yjs] 템플릿 적용 확인', {
      clientId: doc.clientID
    });
    applyLanguageTemplate(matchStore.gameInfo.programmingLanguage);
  },
  { immediate: true }
);

onMounted(async () => {
  await ensureMonacoSetup();
  const monaco = await import('monaco-editor');
  monacoInstance = monaco;
  editor = monaco.editor.create(container.value!, {
    value: '',
    language: resolveMonacoLanguage(matchStore.gameInfo.programmingLanguage),
    theme: 'vs-dark',
    fontSize: 16,
    fontFamily: 'Consolas',
    readOnly: true,
    automaticLayout: true,
  });

  if (ydoc.value) {
    bindYDoc(ydoc.value);
  }
  
  // editor 생성 후 readOnly 상태 즉시 업데이트
  updateReadOnly();
  applyLanguageTemplate(matchStore.gameInfo.programmingLanguage);
});

onUnmounted(() => {
  binding?.destroy();
  editor?.dispose();
  monacoInstance = null;
});
</script>