import{C as e,g as t,wt as n,yt as r}from"./reactivity.esm-bundler-B13GM2Wg.js";import{D as i,E as a,_ as o,b as s,k as c,mt as l,qt as u,v as d,xt as f,y as p}from"./runtime-core.esm-bundler-DY-H7G2c.js";import{t as m,w as h}from"./runtime-dom.esm-bundler-CitBmJMJ.js";import{o as g,s as _,t as v}from"./VDropdown-CVSf2RK8.js";import{t as y}from"./plugin-vue_export-helper-BDNMzG2s.js";import{t as b}from"./VCheckbox-v0QF3frR.js";import{t as x}from"./VCircularProgress-BgCwibqi.js";import{t as S}from"./VFilledButton-C-D-I8--.js";import{t as C}from"./VOutlinedButton-ucKZZXr2.js";import{n as w,t as T}from"./search-rounded-BFDSthlv.js";import{n as ee,t as te}from"./VSelect-BMym6bVD.js";import{t as E}from"./VModal-BPYnBh4W.js";import{n as D,t as O}from"./more-vert-fPcDR3Xa.js";import{n as k,t as A}from"./music-BxraHQcp.js";import{t as j}from"./image-Bcmj4aTQ.js";import{t as M}from"./VTextField-DqmEflF0.js";import{t as N}from"./VPagination-DSR-ZHma.js";import{n as P,t as F}from"./VFilterChip-BIyQ4Mpx.js";import{n as I,t as L}from"./DangerAction-0aEQv1PV.js";import{t as R}from"./MarkdownEditor-hHvhhr9A.js";var z={viewBox:`0 0 24 24`,width:`1.2em`,height:`1.2em`};function B(e,t){return l(),s(`svg`,z,[...t[0]||=[o(`path`,{fill:`currentColor`,d:`M10.825 22q-.675 0-1.162-.45t-.588-1.1L8.85 18.8q-.325-.125-.612-.3t-.563-.375l-1.55.65q-.625.275-1.25.05t-.975-.8l-1.175-2.05q-.35-.575-.2-1.225t.675-1.075l1.325-1Q4.5 12.5 4.5 12.337v-.675q0-.162.025-.337l-1.325-1Q2.675 9.9 2.525 9.25t.2-1.225L3.9 5.975q.35-.575.975-.8t1.25.05l1.55.65q.275-.2.575-.375t.6-.3l.225-1.65q.1-.65.588-1.1T10.825 2h2.35q.675 0 1.163.45t.587 1.1l.225 1.65q.325.125.613.3t.562.375l1.55-.65q.625-.275 1.25-.05t.975.8l1.175 2.05q.35.575.2 1.225t-.675 1.075l-1.325 1q.025.175.025.338v.674q0 .163-.05.338l1.325 1q.525.425.675 1.075t-.2 1.225l-1.2 2.05q-.35.575-.975.8t-1.25-.05l-1.5-.65q-.275.2-.575.375t-.6.3l-.225 1.65q-.1.65-.587 1.1t-1.163.45zm1.225-6.5q1.45 0 2.475-1.025T15.55 12t-1.025-2.475T12.05 8.5q-1.475 0-2.488 1.025T8.55 12t1.013 2.475T12.05 15.5`},null,-1)]])}var V=t({name:`material-symbols-settings-rounded`,render:B}),H={class:`ux-item`},U={class:`ux-demo`},W={class:`ux-code`},G=y(c({__name:`UxCodeBlock`,props:{code:{}},setup(e){return(t,r)=>(l(),s(`div`,H,[o(`div`,U,[f(t.$slots,`default`,{},void 0,!0)]),o(`pre`,W,[o(`code`,null,n(e.code),1)])]))}}),[[`__scopeId`,`data-v-209d99ba`]]),K={class:`ux-section`},q={class:`button-group`},J=c({__name:`UxButtons`,setup(t){let n=e(`a`);return(e,t)=>{let c=S,d=C,f=V,p=g,m=ee,h=w;return l(),s(`section`,K,[t[10]||=o(`h2`,null,`Buttons`,-1),i(G,{code:`<v-filled-button>Primary</v-filled-button>`},{default:u(()=>[i(c,null,{default:u(()=>[...t[3]||=[a(`Primary`,-1)]]),_:1}),i(c,{disabled:``},{default:u(()=>[...t[4]||=[a(`Disabled`,-1)]]),_:1}),i(c,{loading:!0},{default:u(()=>[...t[5]||=[a(`Loading`,-1)]]),_:1})]),_:1}),i(G,{code:`<v-outlined-button>Secondary</v-outlined-button>`},{default:u(()=>[i(d,null,{default:u(()=>[...t[6]||=[a(`Secondary`,-1)]]),_:1}),i(d,{disabled:``},{default:u(()=>[...t[7]||=[a(`Disabled`,-1)]]),_:1}),i(d,{loading:!0},{default:u(()=>[...t[8]||=[a(`Loading`,-1)]]),_:1})]),_:1}),i(G,{code:`<v-outlined-button class="danger">Danger</v-outlined-button>`},{default:u(()=>[i(d,{class:`danger`},{default:u(()=>[...t[9]||=[a(`Danger`,-1)]]),_:1})]),_:1}),i(G,{code:`<v-icon-button>
  <i-material-symbols:settings-rounded />
</v-icon-button>`},{default:u(()=>[i(p,null,{default:u(()=>[i(f)]),_:1}),i(p,null,{default:u(()=>[i(m)]),_:1}),i(p,null,{default:u(()=>[i(h)]),_:1}),i(p,{loading:!0})]),_:1}),t[11]||=o(`h3`,null,`.button-group`,-1),i(G,{code:`<div class="button-group">
  <button :class="{ selected: active === 'a' }">A</button>
  <button :class="{ selected: active === 'b' }">B</button>
</div>`},{default:u(()=>[o(`div`,q,[o(`button`,{class:r({selected:n.value===`a`}),onClick:t[0]||=e=>n.value=`a`},`Option A`,2),o(`button`,{class:r({selected:n.value===`b`}),onClick:t[1]||=e=>n.value=`b`},`Option B`,2),o(`button`,{class:r({selected:n.value===`c`}),onClick:t[2]||=e=>n.value=`c`},`Option C`,2)])]),_:1})])}}}),Y={class:`ux-section`},X={class:`form-row`,style:{width:`100%`}},Z=`<v-select v-model="val" label="Select" :options="[
  { label: 'Option 1', value: '1' },
  { label: 'Option 2', value: '2' },
]" />`,Q=c({__name:`UxForms`,setup(t){let r=e(``),c=e(``),d=e(``),f=e(``),p=e(``),m=e(!1),h=e(``),g=e(``),_=[{label:`Option 1`,value:`1`},{label:`Option 2`,value:`2`},{label:`Option 3`,value:`3`}];return(e,t)=>{let v=M,y=te,x=b;return l(),s(`section`,Y,[t[13]||=o(`h2`,null,`Form Elements`,-1),i(G,{code:`<v-text-field v-model="text" label="Label" />`},{default:u(()=>[i(v,{modelValue:r.value,"onUpdate:modelValue":t[0]||=e=>r.value=e,label:`Label`},null,8,[`modelValue`])]),_:1}),i(G,{code:`<v-text-field v-model="text" placeholder="Placeholder" />`},{default:u(()=>[i(v,{modelValue:c.value,"onUpdate:modelValue":t[1]||=e=>c.value=e,placeholder:`Enter something...`},null,8,[`modelValue`])]),_:1}),i(G,{code:`<v-text-field v-model="text" error error-text="Required field" />`},{default:u(()=>[i(v,{modelValue:d.value,"onUpdate:modelValue":t[2]||=e=>d.value=e,label:`With Error`,error:``,"error-text":`This field is required`},null,8,[`modelValue`])]),_:1}),i(G,{code:`<v-text-field v-model="text" type="textarea" :rows="3" label="Textarea" />`},{default:u(()=>[i(v,{modelValue:f.value,"onUpdate:modelValue":t[3]||=e=>f.value=e,type:`textarea`,rows:3,label:`Textarea`},null,8,[`modelValue`])]),_:1}),i(G,{code:Z},{default:u(()=>[i(y,{modelValue:p.value,"onUpdate:modelValue":t[4]||=e=>p.value=e,label:`Select`,options:_},null,8,[`modelValue`])]),_:1}),i(G,{code:`<v-checkbox v-model="checked" />`},{default:u(()=>[i(x,{modelValue:m.value,"onUpdate:modelValue":t[5]||=e=>m.value=e},null,8,[`modelValue`]),t[8]||=a(),o(`span`,null,`Checkbox (`+n(m.value?`checked`:`unchecked`)+`)`,1)]),_:1}),i(G,{code:`<v-checkbox indeterminate />`},{default:u(()=>[i(x,{indeterminate:``}),t[9]||=a(),t[10]||=o(`span`,null,`Indeterminate`,-1)]),_:1}),t[14]||=o(`h3`,null,`CSS: .form-row / .form-label`,-1),i(G,{code:`<div class="form-row">
  <div><label class="form-label">Name</label>
    <v-text-field ... /></div>
  <div><label class="form-label">Email</label>
    <v-text-field ... /></div>
</div>`},{default:u(()=>[o(`div`,X,[o(`div`,null,[t[11]||=o(`label`,{class:`form-label`},`Name`,-1),i(v,{modelValue:h.value,"onUpdate:modelValue":t[6]||=e=>h.value=e,placeholder:`Name`},null,8,[`modelValue`])]),o(`div`,null,[t[12]||=o(`label`,{class:`form-label`},`Email`,-1),i(v,{modelValue:g.value,"onUpdate:modelValue":t[7]||=e=>g.value=e,placeholder:`Email`},null,8,[`modelValue`])])])]),_:1})])}}}),ne={class:`ux-section`},re={style:{width:`100%`}},ie=c({__name:`UxFeedback`,setup(e){return(e,t)=>{let n=x,r=I,a=L;return l(),s(`section`,ne,[t[3]||=o(`h2`,null,`Feedback & Progress`,-1),i(G,{code:`<v-circular-progress indeterminate />
<v-circular-progress indeterminate class="sm" />`},{default:u(()=>[i(n,{indeterminate:``}),i(n,{indeterminate:``,class:`sm`}),t[0]||=o(`span`,{style:{"font-size":`0.8rem`,color:`var(--md-sys-color-on-surface-variant)`}},`default / .sm`,-1)]),_:1}),i(G,{code:`<v-circular-progress indeterminate class="primary" />
<v-circular-progress indeterminate class="error" />`},{default:u(()=>[i(n,{indeterminate:``,class:`primary`}),i(n,{indeterminate:``,class:`secondary`}),i(n,{indeterminate:``,class:`error`})]),_:1}),i(G,{code:`<progress-card label-html="Uploading <b>file.zip</b>" :value="65" />`},{default:u(()=>[i(r,{"label-html":`Uploading <b>file.zip</b>`,value:65})]),_:1}),t[4]||=o(`h3`,null,`CSS: .progress-track / .progress-fill`,-1),i(G,{code:`<div class="progress-track">
  <div class="progress-fill" :style="{ width: '60%' }" />
</div>`},{default:u(()=>[...t[1]||=[o(`div`,{style:{width:`100%`}},[o(`div`,{class:`progress-track`},[o(`div`,{class:`progress-fill`,style:{width:`60%`}})])],-1)]]),_:1}),t[5]||=o(`h3`,null,`<danger-action>`,-1),i(G,{code:`<danger-action label="Delete account" confirm-text="Are you sure?" @confirm="..." />`},{default:u(()=>[o(`div`,re,[i(a,{label:`Delete account`,"confirm-text":`Are you sure?`,onConfirm:()=>{}})])]),_:1}),t[6]||=o(`h3`,null,`CSS: .alert-danger / .alert-info / .alert-warning`,-1),i(G,{code:`<div class="alert-info show">Info alert message</div>
<div class="alert-danger show">Danger alert</div>
<div class="alert-warning show">Warning alert</div>`},{default:u(()=>[...t[2]||=[o(`div`,{style:{width:`100%`,display:`flex`,"flex-direction":`column`,gap:`8px`}},[o(`div`,{class:`alert-info show`},`Info: Something informational happened.`),o(`div`,{class:`alert-danger show`},[o(`svg`,{xmlns:`http://www.w3.org/2000/svg`,width:`20`,height:`20`,viewBox:`0 0 24 24`,fill:`none`,stroke:`currentColor`,"stroke-width":`2`},[o(`circle`,{cx:`12`,cy:`12`,r:`10`}),o(`line`,{x1:`15`,y1:`9`,x2:`9`,y2:`15`}),o(`line`,{x1:`9`,y1:`9`,x2:`15`,y2:`15`})]),o(`div`,{class:`alert-body`},`Danger: Something went wrong.`)])],-1)]]),_:1})])}}}),ae={class:`ux-section`},oe=`<v-modal v-if="show" @close="show = false">
  <template #headline>Title</template>
  <template #content>Content here</template>
  <template #actions>
    <v-outlined-button @click="show = false">Cancel</v-outlined-button>
    <v-filled-button @click="show = false">OK</v-filled-button>
  </template>
</v-modal>`,se=`<v-dropdown v-model="open">
  <template #trigger>
    <v-outlined-button>Menu</v-outlined-button>
  </template>
  <div class="dropdown-item">Action One</div>
  <div class="dropdown-item selected">Selected</div>
  <div class="dropdown-item danger">Danger</div>
</v-dropdown>`,ce=c({__name:`UxOverlays`,setup(t){let n=e(!1),r=e(!1),c=e(!1);return(e,t)=>{let f=S,m=C,h=E,_=v,y=O,b=g;return l(),s(`section`,ae,[t[19]||=o(`h2`,null,`Overlays`,-1),i(G,{code:oe},{default:u(()=>[i(f,{onClick:t[0]||=e=>n.value=!0},{default:u(()=>[...t[13]||=[a(`Open Modal`,-1)]]),_:1}),n.value?(l(),d(h,{key:0,onClose:t[3]||=e=>n.value=!1},{headline:u(()=>[...t[14]||=[a(`Modal Title`,-1)]]),content:u(()=>[...t[15]||=[o(`p`,null,`This is the modal content area. You can put any content here.`,-1)]]),actions:u(()=>[i(m,{onClick:t[1]||=e=>n.value=!1},{default:u(()=>[...t[16]||=[a(`Cancel`,-1)]]),_:1}),i(f,{onClick:t[2]||=e=>n.value=!1},{default:u(()=>[...t[17]||=[a(`Confirm`,-1)]]),_:1})]),_:1})):p(``,!0)]),_:1}),t[20]||=o(`h3`,null,`<v-dropdown> + .dropdown-item`,-1),i(G,{code:se},{default:u(()=>[i(_,{modelValue:r.value,"onUpdate:modelValue":t[9]||=e=>r.value=e},{trigger:u(()=>[i(m,{onClick:t[4]||=e=>r.value=!r.value},{default:u(()=>[...t[18]||=[a(`Open Dropdown`,-1)]]),_:1})]),default:u(()=>[o(`div`,{class:`dropdown-item`,onClick:t[5]||=e=>r.value=!1},`Action One`),o(`div`,{class:`dropdown-item`,onClick:t[6]||=e=>r.value=!1},`Action Two`),o(`div`,{class:`dropdown-item selected`,onClick:t[7]||=e=>r.value=!1},`Selected Item`),o(`div`,{class:`dropdown-item danger`,onClick:t[8]||=e=>r.value=!1},`Danger Item`)]),_:1},8,[`modelValue`])]),_:1}),i(G,{code:`<!-- Dropdown with icon button trigger -->
<v-dropdown v-model="open">
  <template #trigger>
    <v-icon-button>
      <i-material-symbols:more-vert />
    </v-icon-button>
  </template>
  <div class="dropdown-item">Edit</div>
  <div class="dropdown-item danger">Delete</div>
</v-dropdown>`},{default:u(()=>[i(_,{modelValue:c.value,"onUpdate:modelValue":t[12]||=e=>c.value=e},{trigger:u(()=>[i(b,null,{default:u(()=>[i(y)]),_:1})]),default:u(()=>[o(`div`,{class:`dropdown-item`,onClick:t[10]||=e=>c.value=!1},`Edit`),o(`div`,{class:`dropdown-item danger`,onClick:t[11]||=e=>c.value=!1},`Delete`)]),_:1},8,[`modelValue`])]),_:1})])}}}),le=[`tabindex`],ue={key:0,class:`v-input-chip__icon`},de={class:`v-input-chip__label`},fe=[`disabled`,`aria-label`],pe=y(c({__name:`VInputChip`,props:{label:{default:``},selected:{type:Boolean,default:!1},disabled:{type:Boolean,default:!1},removeOnly:{type:Boolean,default:!1},ariaLabelRemove:{default:``}},emits:[`remove`,`click`],setup(e,{emit:t}){let i=e,a=t;function c(){!i.disabled&&!i.removeOnly&&a(`click`)}function u(){i.disabled||a(`remove`)}function d(e){(e.key===`Enter`||e.key===` `)&&(e.preventDefault(),c())}return(t,i)=>(l(),s(`div`,{class:r([`v-input-chip`,{"v-input-chip--selected":e.selected,"v-input-chip--disabled":e.disabled,"v-input-chip--remove-only":e.removeOnly}]),tabindex:e.disabled?-1:0,onClick:c,onKeydown:d},[t.$slots.default?(l(),s(`span`,ue,[f(t.$slots,`default`,{},void 0,!0)])):p(``,!0),o(`span`,de,n(e.label),1),o(`button`,{class:`v-input-chip__remove`,type:`button`,disabled:e.disabled,"aria-label":e.ariaLabelRemove||`Remove`,onClick:h(u,[`stop`])},[...i[0]||=[o(`svg`,{viewBox:`0 0 24 24`,width:`18`,height:`18`},[o(`path`,{d:`M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z`})],-1)]],8,fe)],42,le))}}),[[`__scopeId`,`data-v-aa119b53`]]),me={class:`ux-section`},he={class:`top-app-bar`,style:{width:`100%`}},ge={class:`actions`},_e=c({__name:`UxDataDisplay`,setup(t){let n=e(`all`);return(e,t)=>{let r=F,a=P,c=pe,d=D,f=g,p=T,m=O;return l(),s(`section`,me,[t[6]||=o(`h2`,null,`Chips & Data Display`,-1),t[7]||=o(`h3`,null,`<v-filter-chip> + <v-chip-set>`,-1),i(G,{code:`<v-chip-set>
  <v-filter-chip label="All" :selected="filter === 'all'" @click="filter = 'all'" />
  <v-filter-chip label="Active" :selected="filter === 'active'" @click="filter = 'active'" />
</v-chip-set>`},{default:u(()=>[i(a,null,{default:u(()=>[i(r,{label:`All`,selected:n.value===`all`,onClick:t[0]||=e=>n.value=`all`},null,8,[`selected`]),i(r,{label:`Active`,selected:n.value===`active`,onClick:t[1]||=e=>n.value=`active`},null,8,[`selected`]),i(r,{label:`Archived`,selected:n.value===`archived`,onClick:t[2]||=e=>n.value=`archived`},null,8,[`selected`])]),_:1})]),_:1}),t[8]||=o(`h3`,null,`<v-input-chip>`,-1),i(G,{code:`<v-input-chip label="Tag" @remove="..." />`},{default:u(()=>[i(c,{label:`Photo`,onRemove:()=>{}}),i(c,{label:`Video`,selected:``,onRemove:()=>{}}),i(c,{label:`Disabled`,disabled:``})]),_:1}),t[9]||=o(`h3`,null,`CSS: .card / .card.outlined`,-1),i(G,{code:`<div class="card">Default card</div>
<div class="card outlined">Outlined card</div>
<div class="card selected">Selected card</div>`},{default:u(()=>[...t[3]||=[o(`div`,{class:`card`,style:{flex:`1`}},`Default card content`,-1),o(`div`,{class:`card outlined`,style:{flex:`1`}},`Outlined card content`,-1),o(`div`,{class:`card selected`,style:{flex:`1`}},`Selected card content`,-1)]]),_:1}),t[10]||=o(`h3`,null,`CSS: .selectable-card`,-1),i(G,{code:`<div class="selectable-card">Normal</div>
<div class="selectable-card selected">Selected</div>
<div class="selectable-card selecting">Selecting</div>`},{default:u(()=>[...t[4]||=[o(`div`,{class:`selectable-card`,style:{padding:`12px`,flex:`1`}},`Normal`,-1),o(`div`,{class:`selectable-card selected`,style:{padding:`12px`,flex:`1`}},`Selected`,-1),o(`div`,{class:`selectable-card selecting`,style:{padding:`12px`,flex:`1`}},`Selecting`,-1)]]),_:1}),t[11]||=o(`h3`,null,`CSS: .top-app-bar`,-1),i(G,{code:`<div class="top-app-bar">
  <v-icon-button>...</v-icon-button>
  <div class="title">Page Title</div>
  <div class="actions">...</div>
</div>`},{default:u(()=>[o(`div`,he,[i(f,null,{default:u(()=>[i(d)]),_:1}),t[5]||=o(`div`,{class:`title`},`Page Title`,-1),o(`div`,ge,[i(f,null,{default:u(()=>[i(p)]),_:1}),i(f,null,{default:u(()=>[i(m)]),_:1})])])]),_:1})])}}}),ve={class:`ux-section`},ye={style:{display:`flex`,"flex-direction":`column`,gap:`8px`,width:`100%`}},be={style:{display:`flex`,gap:`16px`,"align-items":`flex-start`}},xe={key:0,class:`card`,style:{padding:`8px`}},Se={key:0,class:`card`,style:{padding:`8px`,overflow:`hidden`}},Ce={key:0,class:`card`,style:{padding:`8px`,overflow:`hidden`}},we={class:`nav`,style:{width:`200px`}},Te={class:`active`},Ee={class:`icon`},De={class:`icon`},$={class:`icon`},Oe=c({__name:`UxCssUtilities`,setup(t){let r=e(!0),c=e(3);return(e,t)=>{let d=C,f=k,h=j,g=A,_=N;return l(),s(`section`,ve,[t[12]||=o(`h2`,null,`CSS Utilities`,-1),t[13]||=o(`h3`,null,`.skeleton-text / .skeleton-image`,-1),i(G,{code:`<div class="skeleton-text" style="width: 60%" />
<div class="skeleton-text lg" style="width: 40%" />
<div class="skeleton-image lg" />
<div class="skeleton-checkbox" />`},{default:u(()=>[...t[1]||=[o(`div`,{style:{display:`flex`,gap:`12px`,"align-items":`center`,width:`100%`}},[o(`div`,{class:`skeleton-image lg`}),o(`div`,{style:{flex:`1`,display:`flex`,"flex-direction":`column`,gap:`8px`}},[o(`div`,{class:`skeleton-text`,style:{width:`70%`}}),o(`div`,{class:`skeleton-text lg`,style:{width:`45%`}})]),o(`div`,{class:`skeleton-checkbox`})],-1)]]),_:1}),t[14]||=o(`h3`,null,`.nowrap`,-1),i(G,{code:`<span class="nowrap">Text that won't wrap</span>`},{default:u(()=>[...t[2]||=[o(`span`,{class:`nowrap`},`This text will not wrap to a new line, no matter what.`,-1)]]),_:1}),t[15]||=o(`h3`,null,`Vue Transitions: fade / width / height`,-1),i(G,{code:`<transition name="fade">
  <div v-if="show">Fades in/out</div>
</transition>

<transition name="width">
  <div v-if="show">Width transition</div>
</transition>

<transition name="height">
  <div v-if="show">Height transition</div>
</transition>`},{default:u(()=>[o(`div`,ye,[i(d,{onClick:t[0]||=e=>r.value=!r.value},{default:u(()=>[a(` Toggle (`+n(r.value?`visible`:`hidden`)+`) `,1)]),_:1}),o(`div`,be,[o(`div`,null,[t[3]||=o(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`fade:`,-1),i(m,{name:`fade`},{default:u(()=>[r.value?(l(),s(`div`,xe,`Fade`)):p(``,!0)]),_:1})]),o(`div`,null,[t[4]||=o(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`width:`,-1),i(m,{name:`width`},{default:u(()=>[r.value?(l(),s(`div`,Se,`Width`)):p(``,!0)]),_:1})]),o(`div`,null,[t[5]||=o(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`height:`,-1),i(m,{name:`height`},{default:u(()=>[r.value?(l(),s(`div`,Ce,`Height`)):p(``,!0)]),_:1})])])])]),_:1}),t[16]||=o(`h3`,null,`.surface-card`,-1),i(G,{code:`<div class="surface-card">Surface card with background</div>`},{default:u(()=>[...t[6]||=[o(`div`,{class:`surface-card`,style:{flex:`1`}},`Surface card content with default padding`,-1)]]),_:1}),t[17]||=o(`h3`,null,`.nav (sidebar navigation)`,-1),i(G,{code:`<ul class="nav">
  <li class="active">
    <div class="icon">📁</div>
    <div class="title">Files</div>
    <div class="count">12</div>
  </li>
</ul>`},{default:u(()=>[o(`ul`,we,[o(`li`,Te,[o(`div`,Ee,[i(f)]),t[7]||=o(`div`,{class:`title`},`Files`,-1),t[8]||=o(`div`,{class:`count`},`12`,-1)]),o(`li`,null,[o(`div`,De,[i(h)]),t[9]||=o(`div`,{class:`title`},`Images`,-1),t[10]||=o(`div`,{class:`count`},`48`,-1)]),o(`li`,null,[o(`div`,$,[i(g)]),t[11]||=o(`div`,{class:`title`},`Audio`,-1)])])]),_:1}),t[18]||=o(`h3`,null,`.pagination`,-1),i(G,{code:`<v-pagination :total="100" :limit="10" :page="3" :go="goToPage" />`},{default:u(()=>[i(_,{total:100,limit:10,page:c.value,go:e=>c.value=e},null,8,[`page`,`go`])]),_:1})])}}}),ke={class:`ux-md-editor`},Ae={class:`toolbar`},je={class:`editor-frame`},Me=y(c({__name:`UxMarkdownEditor`,setup(t){let r=e(`# 产品设计评审

本周与团队同步了 **PlainDesk 2.0** 的整体方向，重点是 *编辑器* 的交互模型。参考资料见 [发布计划](https://example.com)，接口约定在 \`useNoteEdit.ts\`。

## 待办事项

- [x] 整理竞品交互对比
- [ ] 绘制 Live Preview 高保真稿
- [ ] ~~手写富文本解析器~~ 改用 syntaxTree

\`\`\`ts
const view = new EditorView({ state, parent })
\`\`\`

缩进代码：

    indented code line
    second line

![架构图](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='360'%20height='120'%3E%3Crect%20width='360'%20height='120'%20fill='%234f5f9e'/%3E%3Ctext%20x='180'%20y='68'%20font-size='22'%20fill='white'%20text-anchor='middle'%20font-family='sans-serif'%3EPlainDesk%20Architecture%3C/text%3E%3C/svg%3E)

| 指标 | 冷启动 | 滚动 |
| :--- | ---: | :---: |
| 改造前 | 240ms | 12% 掉帧 |
| **工具** | \`vite\` | ~~installed~~ |
| 改造后 | 90ms | 0 掉帧 |

行内公式 $E = mc^2$ 与积分：

$$
\\int_0^\\infty e^{-x} \\, dx = 1
$$

---

---

## Table of Contents

*   [Method 1: PlainApp — no app on the iPhone](#method-1-plainapp-no-app-on-the-iphone)
*   [Method 2: LocalSend — an app on both sides](#method-2-localsend-an-app-on-both-sides)

![Diagram 1](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='360'%20height='80'%3E%3Crect%20width='360'%20height='80'%20fill='%2300897b'/%3E%3Ctext%20x='180'%20y='48'%20font-size='20'%20fill='white'%20text-anchor='middle'%20font-family='sans-serif'%3EDiagram%201%3C/text%3E%3C/svg%3E)

## Method 1: PlainApp — no app on the iPhone

方法一的内容段落。

> 编辑器是笔记应用的主战场。
`),c=e(document.documentElement.classList.contains(`dark`));function u(){c.value=!c.value,document.documentElement.classList.toggle(`dark`,c.value),_.emit(`color_mode_changed`)}return(e,t)=>(l(),s(`section`,ke,[t[1]||=o(`h2`,null,`MarkdownEditor (Live Preview)`,-1),t[2]||=o(`p`,{class:`hint`},[a(` 光标所在元素显示原始 Markdown 标记，移开后原地渲染；空行输入 `),o(`code`,null,`/`),a(` 呼出块插入菜单。 `)],-1),o(`div`,Ae,[o(`button`,{type:`button`,onClick:u},n(c.value?`☀️ Light`:`🌙 Dark`),1)]),o(`div`,je,[i(R,{modelValue:r.value,"onUpdate:modelValue":t[0]||=e=>r.value=e,placeholder:`Write markdown... (input / for blocks)`},null,8,[`modelValue`])])]))}}),[[`__scopeId`,`data-v-8f4d67fe`]]),Ne={class:`ux-page`},Pe={class:`ux-main`},Fe=y(c({__name:`UxView`,setup(e){return(e,t)=>(l(),s(`div`,Ne,[t[0]||=o(`header`,{class:`ux-header`},[o(`h1`,null,`UI Component Library`),o(`p`,null,`Base components and reusable CSS patterns. All components are globally registered with V-prefix.`)],-1),o(`main`,Pe,[i(J),i(Q),i(ie),i(ce),i(_e),i(Oe),i(Me)])]))}}),[[`__scopeId`,`data-v-b578e6b5`]]);export{Fe as default};