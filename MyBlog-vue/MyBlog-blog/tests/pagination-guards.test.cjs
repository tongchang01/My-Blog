const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

function loadTsModule(filePath) {
  const source = fs.readFileSync(filePath, 'utf8')
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2019,
      esModuleInterop: true
    }
  })
  const module = { exports: {} }
  const fn = new Function('require', 'module', 'exports', transpiled.outputText)
  fn(require, module, module.exports)
  return module.exports
}

const helperPath = path.join(__dirname, '..', 'src', 'utils', 'pagination.ts')
const { normalizePageRecords, normalizeListData } = loadTsModule(helperPath)

{
  const page = normalizePageRecords({ data: null })
  assert.deepEqual(page.records, [])
  assert.equal(page.count, 0)
}

{
  const page = normalizePageRecords({ data: { records: null, count: null } })
  assert.deepEqual(page.records, [])
  assert.equal(page.count, 0)
}

{
  const page = normalizePageRecords({
    data: {
      records: [{ id: 1 }, { id: 2 }],
      count: 2
    }
  })
  assert.deepEqual(page.records, [{ id: 1 }, { id: 2 }])
  assert.equal(page.count, 2)
}

{
  const list = normalizeListData({ data: null })
  assert.deepEqual(list, [])
}

{
  const list = normalizeListData({ data: [{ id: 1 }] })
  assert.deepEqual(list, [{ id: 1 }])
}

console.log('pagination guards ok')
