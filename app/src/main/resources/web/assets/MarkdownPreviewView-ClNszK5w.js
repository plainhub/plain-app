import{i as e}from"./chunk-aKtaBQYM.js";import{H as t,T as n,_ as r,b as i,g as a}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{t as o}from"./plugin-vue_export-helper-BDNMzG2s.js";import{a as s,c,d as l,i as u,l as d,n as f,o as p,r as m,s as h,t as g,u as _}from"./markdown-it-task-lists-ByUfBpgM.js";var v=e(_(),1),y=e(d(),1),b=e(c(),1),x=e(h(),1),S=e(p(),1),C=e(s(),1),w=e(u(),1),T=e(m(),1),E=e(g(),1),D={class:`md-preview-page`},O={class:`md-preview-main`},k=[`innerHTML`],A=`# Heading 1

## Heading 2

### Heading 3

#### Heading 4

This is a regular paragraph with some content. It demonstrates the unified markdown reading theme. The quick brown fox jumps over the lazy dog. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

---

## Text Formatting

You can use **bold text**, *italic text*, and ***both***. There's also ~~strikethrough~~ and \`inline code\` formatting. superscript: x^2^, subscript: H~2~O, ++inserted text++, and ==marked text==.

## Lists

### Unordered List
- First item
- Second item
  - Nested item
  - Another nested item
- Third item

### Ordered List
1. First step
2. Second step
3. Third step

### Task List
- [x] Completed task
- [x] Another completed task
- [ ] Pending task

## Code Blocks

\`\`\`kotlin
fun main() {
    val greeting = "Hello, Markdown!"
    println(greeting)
}
\`\`\`

\`\`\`javascript
function fibonacci(n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}
\`\`\`

## Blockquotes

> "This is a blockquote. It's often used to highlight important information or quotes from other sources."
> - Source attribution

> Nested blockquote with multiple lines.
> Each line starts with a > character.

## Tables

| Feature | Description |
|---------|-------------|
| Headings | Clear hierarchy with proper spacing |
| Code | Dark background with monospace font |
| Quote | Muted text with left accent border |
| Table | Clean borders, comfortable rows with rounded corners |

## Links

[Visit PlainApp](https://plainapp.app) for more information.

## Images

![Placeholder Image](https://picsum.photos/600/300)

## Math

Inline math: $E = mc^2$

Block math:
$$
\\int_0^1 x^2 dx = \\frac{1}{3}
$$

## Definition List

Term 1
: Definition for term 1

Term 2
: Definition for term 2

## Footnotes

Here is a footnote reference[^1].

[^1]: This is the footnote content.
`,j=o(n({__name:`MarkdownPreviewView`,setup(e){let n=new l({html:!0,xhtmlOut:!0,breaks:!0,linkify:!0,typographer:!0}).use(v.default).use(y.default).use(b.default).use(x.default).use(S.default).use(C.default).use(w.default).use(T.default,{engine:f,delimiters:`dollars`,katexOptions:{output:`html`}}).use(E.default,{enabled:!0}),o=a(()=>n.render(A));return(e,n)=>(t(),i(`div`,D,[n[0]||=r(`header`,{class:`md-preview-header`},[r(`h1`,null,`Markdown Preview`)],-1),r(`main`,O,[r(`div`,{class:`md-container`,innerHTML:o.value},null,8,k)])]))}}),[[`__scopeId`,`data-v-ee1c1018`]]);export{j as default};