#!/usr/bin/env node

// Import mpp-core
import MppCore from '../../mpp-core/build/packages/js';

console.log('\n🔍 Inspecting MppCore exports\n');
console.log('═'.repeat(60));

console.log('\n📦 Top-level exports:');
console.log(Object.keys(MppCore));

console.log('\n📦 MppCore.cc:');
console.log(Object.keys(MppCore.cc || {}));

console.log('\n📦 MppCore.cc.unitmesh:');
console.log(Object.keys(MppCore.cc?.unitmesh || {}));

console.log('\n📦 MppCore.cc.unitmesh.agent:');
console.log(Object.keys(MppCore.cc?.unitmesh?.agent || {}));

console.log('\n📦 MppCore.cc.unitmesh.agent.subagent:');
console.log(Object.keys(MppCore.cc?.unitmesh?.agent?.subagent || {}));

console.log('\n📦 MppCore.cc.unitmesh.agent.platform:');
console.log(Object.keys(MppCore.cc?.unitmesh?.agent?.platform || {}));

console.log('\n');
