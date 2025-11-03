#!/usr/bin/env node

import MppCore from '../../mpp-core/build/packages/js';

console.log('\n📦 MppCore.cc.unitmesh.llm exports:');
console.log(Object.keys(MppCore.cc?.unitmesh?.llm || {}));

console.log('\n🔍 Looking for LLM Service...');
const llm = MppCore.cc.unitmesh.llm;
for (const key of Object.keys(llm)) {
  if (key.toLowerCase().includes('llm') || key.toLowerCase().includes('service')) {
    console.log(`  - ${key}: ${typeof llm[key]}`);
  }
}
