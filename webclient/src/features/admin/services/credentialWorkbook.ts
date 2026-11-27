import { strToU8, zipSync } from 'fflate';
import type { IssuedAccountCredential } from '@/types/auth.types';

const xml = (text: string) => [...text].filter(char => {
  const cp = char.codePointAt(0)!;
  return cp === 9 || cp === 10 || cp === 13 || (cp >= 32 && cp !== 0xfffe && cp !== 0xffff);
}).join('').replace(/_x[0-9a-f]{4}_/gi, escape => `_x005F_${escape.slice(1)}`)
  .replace(/\r/g, '_x000D_').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;').replace(/'/g, '&apos;');

/** Minimal XLSX, with explicit text cells: preserve passwords, leading zeroes and formula-like values. */
export function createCredentialWorkbook(credentials: readonly IssuedAccountCredential[]) {
  const rows = [
    ['Họ và tên', 'MSSV / MSGV', 'Email đăng nhập', 'Mật khẩu tạm'],
    ...credentials.map(item => [item.fullName ?? '', item.studentLecturerCode ?? '', item.email, item.temporaryPassword]),
  ];
  const strings = rows.flat();
  const cells = rows.map((row, index) => `<row r="${index + 1}">${row.map((_, column) =>
    `<c r="${String.fromCharCode(65 + column)}${index + 1}" t="s" s="${index === 0 ? 1 : 0}"><v>${index * 4 + column}</v></c>`).join('')}</row>`).join('');
  const files: Record<string, string> = {
    '[Content_Types].xml': '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>',
    '_rels/.rels': '<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
    'xl/workbook.xml': '<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Tài khoản" sheetId="1" r:id="rId1"/></sheets></workbook>',
    'xl/_rels/workbook.xml.rels': '<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/></Relationships>',
    'xl/sharedStrings.xml': `<?xml version="1.0" encoding="UTF-8"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.length}" uniqueCount="${strings.length}">${strings.map(value => `<si><t xml:space="preserve">${xml(value)}</t></si>`).join('')}</sst>`,
    'xl/styles.xml': '<?xml version="1.0"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="49" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>',
    'xl/worksheets/sheet1.xml': `<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols><col min="1" max="1" width="30" customWidth="1"/><col min="2" max="2" width="20" customWidth="1"/><col min="3" max="3" width="48" customWidth="1"/><col min="4" max="4" width="32" customWidth="1"/></cols><sheetData>${cells}</sheetData><autoFilter ref="A1:D${rows.length}"/></worksheet>`,
  };
  return zipSync(Object.fromEntries(Object.entries(files).map(([name, data]) => [name, strToU8(data)])));
}

export function downloadCredentialWorkbook(credentials: readonly IssuedAccountCredential[]) {
  const bytes = createCredentialWorkbook(credentials);
  const url = URL.createObjectURL(new Blob([bytes.buffer as ArrayBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `sqb-tai-khoan-${new Date().toISOString().replace(/[:.]/g, '-')}.xlsx`;
  document.body.append(link);
  link.click();
  link.remove();
  // Give the browser time to acquire the Blob before revoking its URL.
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
