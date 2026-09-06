import{$ as e,C as t,G as n,H as r,T as i,_ as a,at as o,b as s,ct as c,d as l,ht as u,n as d,v as f,vt as p,w as m,y as h}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{t as g}from"./VCircularProgress-bkw4GMSv.js";import{t as _}from"./plugin-vue_export-helper-BDNMzG2s.js";import{Ct as v,Ia as y,Kn as b,Na as x,Pa as S,Un as C,_ as w,br as ee,cr as te,ka as T,l as E,or as D,p as O,qa as k,xr as A,xt as j,yr as M}from"./index-gp-GXSgs.js";import{t as N}from"./VPagination-DP_OPvUi.js";import{n as P,t as F}from"./VFilterChip-Cfd7DBzG.js";import{n as I,t as L}from"./DangerAction-CYuhnKYE.js";import{t as R}from"./MarkdownEditor-Ci-QchHN.js";var z={viewBox:`0 0 24 24`,width:`1.2em`,height:`1.2em`};function B(e,t){return r(),s(`svg`,z,[...t[0]||=[a(`path`,{fill:`currentColor`,d:`M10.825 22q-.675 0-1.162-.45t-.588-1.1L8.85 18.8q-.325-.125-.612-.3t-.563-.375l-1.55.65q-.625.275-1.25.05t-.975-.8l-1.175-2.05q-.35-.575-.2-1.225t.675-1.075l1.325-1Q4.5 12.5 4.5 12.337v-.675q0-.162.025-.337l-1.325-1Q2.675 9.9 2.525 9.25t.2-1.225L3.9 5.975q.35-.575.975-.8t1.25.05l1.55.65q.275-.2.575-.375t.6-.3l.225-1.65q.1-.65.588-1.1T10.825 2h2.35q.675 0 1.163.45t.587 1.1l.225 1.65q.325.125.613.3t.562.375l1.55-.65q.625-.275 1.25-.05t.975.8l1.175 2.05q.35.575.2 1.225t-.675 1.075l-1.325 1q.025.175.025.338v.674q0 .163-.05.338l1.325 1q.525.425.675 1.075t-.2 1.225l-1.2 2.05q-.35.575-.975.8t-1.25-.05l-1.5-.65q-.275.2-.575.375t-.6.3l-.225 1.65q-.1.65-.587 1.1t-1.163.45zm1.225-6.5q1.45 0 2.475-1.025T15.55 12t-1.025-2.475T12.05 8.5q-1.475 0-2.488 1.025T8.55 12t1.013 2.475T12.05 15.5`},null,-1)]])}var V=o({name:`material-symbols-settings-rounded`,render:B}),H={class:`ux-item`},U={class:`ux-demo`},W={class:`ux-code`},G=_(i({__name:`UxCodeBlock`,props:{code:{}},setup(e){return(t,i)=>(r(),s(`div`,H,[a(`div`,U,[n(t.$slots,`default`,{},void 0,!0)]),a(`pre`,W,[a(`code`,null,p(e.code),1)])]))}}),[[`__scopeId`,`data-v-209d99ba`]]),K={class:`ux-section`},q={class:`button-group`},J=i({__name:`UxButtons`,setup(n){let i=c(`a`);return(n,o)=>{let c=S,l=x,d=V,f=y,p=b,h=T;return r(),s(`section`,K,[o[14]||=a(`h2`,null,`Buttons`,-1),m(G,{code:`<v-filled-button>Primary</v-filled-button>`},{default:e(()=>[m(c,null,{default:e(()=>[...o[3]||=[t(`Primary`,-1)]]),_:1}),m(c,{disabled:``},{default:e(()=>[...o[4]||=[t(`Disabled`,-1)]]),_:1}),m(c,{loading:!0},{default:e(()=>[...o[5]||=[t(`Loading`,-1)]]),_:1})]),_:1}),m(G,{code:`<v-filled-button class="btn-sm">Small</v-filled-button>
<v-filled-button class="btn-lg">Large</v-filled-button>`},{default:e(()=>[m(c,{class:`btn-sm`},{default:e(()=>[...o[6]||=[t(`Small`,-1)]]),_:1}),m(c,null,{default:e(()=>[...o[7]||=[t(`Default`,-1)]]),_:1}),m(c,{class:`btn-lg`},{default:e(()=>[...o[8]||=[t(`Large`,-1)]]),_:1})]),_:1}),m(G,{code:`<v-outlined-button>Secondary</v-outlined-button>`},{default:e(()=>[m(l,null,{default:e(()=>[...o[9]||=[t(`Secondary`,-1)]]),_:1}),m(l,{disabled:``},{default:e(()=>[...o[10]||=[t(`Disabled`,-1)]]),_:1}),m(l,{loading:!0},{default:e(()=>[...o[11]||=[t(`Loading`,-1)]]),_:1})]),_:1}),m(G,{code:`<v-outlined-button class="danger">Danger</v-outlined-button>
<v-outlined-button class="btn-sm danger">Small Danger</v-outlined-button>`},{default:e(()=>[m(l,{class:`danger`},{default:e(()=>[...o[12]||=[t(`Danger`,-1)]]),_:1}),m(l,{class:`btn-sm danger`},{default:e(()=>[...o[13]||=[t(`Small Danger`,-1)]]),_:1})]),_:1}),m(G,{code:`<v-icon-button>
  <i-material-symbols:settings-rounded />
</v-icon-button>`},{default:e(()=>[m(f,null,{default:e(()=>[m(d)]),_:1}),m(f,null,{default:e(()=>[m(p)]),_:1}),m(f,null,{default:e(()=>[m(h)]),_:1}),m(f,{loading:!0})]),_:1}),o[15]||=a(`h3`,null,`.button-group`,-1),m(G,{code:`<div class="button-group">
  <button :class="{ selected: active === 'a' }">A</button>
  <button :class="{ selected: active === 'b' }">B</button>
</div>`},{default:e(()=>[a(`div`,q,[a(`button`,{class:u({selected:i.value===`a`}),onClick:o[0]||=e=>i.value=`a`},`Option A`,2),a(`button`,{class:u({selected:i.value===`b`}),onClick:o[1]||=e=>i.value=`b`},`Option B`,2),a(`button`,{class:u({selected:i.value===`c`}),onClick:o[2]||=e=>i.value=`c`},`Option C`,2)])]),_:1})])}}}),Y={class:`ux-section`},X={class:`form-row`,style:{width:`100%`}},Z=`<v-select v-model="val" label="Select" :options="[
  { label: 'Option 1', value: '1' },
  { label: 'Option 2', value: '2' },
]" />`,Q=i({__name:`UxForms`,setup(n){let i=c(``),o=c(``),l=c(``),u=c(``),d=c(``),f=c(!1),h=c(``),g=c(``),_=[{label:`Option 1`,value:`1`},{label:`Option 2`,value:`2`},{label:`Option 3`,value:`3`}];return(n,c)=>{let v=j,y=E,b=D;return r(),s(`section`,Y,[c[13]||=a(`h2`,null,`Form Elements`,-1),m(G,{code:`<v-text-field v-model="text" label="Label" />`},{default:e(()=>[m(v,{modelValue:i.value,"onUpdate:modelValue":c[0]||=e=>i.value=e,label:`Label`},null,8,[`modelValue`])]),_:1}),m(G,{code:`<v-text-field v-model="text" placeholder="Placeholder" />`},{default:e(()=>[m(v,{modelValue:o.value,"onUpdate:modelValue":c[1]||=e=>o.value=e,placeholder:`Enter something...`},null,8,[`modelValue`])]),_:1}),m(G,{code:`<v-text-field v-model="text" error error-text="Required field" />`},{default:e(()=>[m(v,{modelValue:l.value,"onUpdate:modelValue":c[2]||=e=>l.value=e,label:`With Error`,error:``,"error-text":`This field is required`},null,8,[`modelValue`])]),_:1}),m(G,{code:`<v-text-field v-model="text" type="textarea" :rows="3" label="Textarea" />`},{default:e(()=>[m(v,{modelValue:u.value,"onUpdate:modelValue":c[3]||=e=>u.value=e,type:`textarea`,rows:3,label:`Textarea`},null,8,[`modelValue`])]),_:1}),m(G,{code:Z},{default:e(()=>[m(y,{modelValue:d.value,"onUpdate:modelValue":c[4]||=e=>d.value=e,label:`Select`,options:_},null,8,[`modelValue`])]),_:1}),m(G,{code:`<v-checkbox v-model="checked" />`},{default:e(()=>[m(b,{modelValue:f.value,"onUpdate:modelValue":c[5]||=e=>f.value=e},null,8,[`modelValue`]),c[8]||=t(),a(`span`,null,`Checkbox (`+p(f.value?`checked`:`unchecked`)+`)`,1)]),_:1}),m(G,{code:`<v-checkbox indeterminate />`},{default:e(()=>[m(b,{indeterminate:``}),c[9]||=t(),c[10]||=a(`span`,null,`Indeterminate`,-1)]),_:1}),c[14]||=a(`h3`,null,`CSS: .form-row / .form-label`,-1),m(G,{code:`<div class="form-row">
  <div><label class="form-label">Name</label>
    <v-text-field ... /></div>
  <div><label class="form-label">Email</label>
    <v-text-field ... /></div>
</div>`},{default:e(()=>[a(`div`,X,[a(`div`,null,[c[11]||=a(`label`,{class:`form-label`},`Name`,-1),m(v,{modelValue:h.value,"onUpdate:modelValue":c[6]||=e=>h.value=e,placeholder:`Name`},null,8,[`modelValue`])]),a(`div`,null,[c[12]||=a(`label`,{class:`form-label`},`Email`,-1),m(v,{modelValue:g.value,"onUpdate:modelValue":c[7]||=e=>g.value=e,placeholder:`Email`},null,8,[`modelValue`])])])]),_:1})])}}}),ne={class:`ux-section`},re={style:{width:`100%`}},ie=i({__name:`UxFeedback`,setup(t){return(t,n)=>{let i=g,o=I,c=L;return r(),s(`section`,ne,[n[3]||=a(`h2`,null,`Feedback & Progress`,-1),m(G,{code:`<v-circular-progress indeterminate />
<v-circular-progress indeterminate class="sm" />`},{default:e(()=>[m(i,{indeterminate:``}),m(i,{indeterminate:``,class:`sm`}),n[0]||=a(`span`,{style:{"font-size":`0.8rem`,color:`var(--md-sys-color-on-surface-variant)`}},`default / .sm`,-1)]),_:1}),m(G,{code:`<v-circular-progress indeterminate class="primary" />
<v-circular-progress indeterminate class="error" />`},{default:e(()=>[m(i,{indeterminate:``,class:`primary`}),m(i,{indeterminate:``,class:`secondary`}),m(i,{indeterminate:``,class:`error`})]),_:1}),m(G,{code:`<progress-card label-html="Uploading <b>file.zip</b>" :value="65" />`},{default:e(()=>[m(o,{"label-html":`Uploading <b>file.zip</b>`,value:65})]),_:1}),n[4]||=a(`h3`,null,`CSS: .progress-track / .progress-fill`,-1),m(G,{code:`<div class="progress-track">
  <div class="progress-fill" :style="{ width: '60%' }" />
</div>`},{default:e(()=>[...n[1]||=[a(`div`,{style:{width:`100%`}},[a(`div`,{class:`progress-track`},[a(`div`,{class:`progress-fill`,style:{width:`60%`}})])],-1)]]),_:1}),n[5]||=a(`h3`,null,`<danger-action>`,-1),m(G,{code:`<danger-action label="Delete account" confirm-text="Are you sure?" @confirm="..." />`},{default:e(()=>[a(`div`,re,[m(c,{label:`Delete account`,"confirm-text":`Are you sure?`,onConfirm:()=>{}})])]),_:1}),n[6]||=a(`h3`,null,`CSS: .alert-danger / .alert-info / .alert-warning`,-1),m(G,{code:`<div class="alert-info show">Info alert message</div>
<div class="alert-danger show">Danger alert</div>
<div class="alert-warning show">Warning alert</div>`},{default:e(()=>[...n[2]||=[a(`div`,{style:{width:`100%`,display:`flex`,"flex-direction":`column`,gap:`8px`}},[a(`div`,{class:`alert-info show`},`Info: Something informational happened.`),a(`div`,{class:`alert-danger show`},[a(`svg`,{xmlns:`http://www.w3.org/2000/svg`,width:`20`,height:`20`,viewBox:`0 0 24 24`,fill:`none`,stroke:`currentColor`,"stroke-width":`2`},[a(`circle`,{cx:`12`,cy:`12`,r:`10`}),a(`line`,{x1:`15`,y1:`9`,x2:`9`,y2:`15`}),a(`line`,{x1:`9`,y1:`9`,x2:`15`,y2:`15`})]),a(`div`,{class:`alert-body`},`Danger: Something went wrong.`)])],-1)]]),_:1})])}}}),ae={class:`ux-section`},oe=`<v-modal v-if="show" @close="show = false">
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
</v-dropdown>`,ce=i({__name:`UxOverlays`,setup(n){let i=c(!1),o=c(!1),l=c(!1);return(n,c)=>{let u=S,d=x,p=v,g=te,_=O,b=y;return r(),s(`section`,ae,[c[19]||=a(`h2`,null,`Overlays`,-1),m(G,{code:oe},{default:e(()=>[m(u,{onClick:c[0]||=e=>i.value=!0},{default:e(()=>[...c[13]||=[t(`Open Modal`,-1)]]),_:1}),i.value?(r(),f(p,{key:0,onClose:c[3]||=e=>i.value=!1},{headline:e(()=>[...c[14]||=[t(`Modal Title`,-1)]]),content:e(()=>[...c[15]||=[a(`p`,null,`This is the modal content area. You can put any content here.`,-1)]]),actions:e(()=>[m(d,{onClick:c[1]||=e=>i.value=!1},{default:e(()=>[...c[16]||=[t(`Cancel`,-1)]]),_:1}),m(u,{onClick:c[2]||=e=>i.value=!1},{default:e(()=>[...c[17]||=[t(`Confirm`,-1)]]),_:1})]),_:1})):h(``,!0)]),_:1}),c[20]||=a(`h3`,null,`<v-dropdown> + .dropdown-item`,-1),m(G,{code:se},{default:e(()=>[m(g,{modelValue:o.value,"onUpdate:modelValue":c[9]||=e=>o.value=e},{trigger:e(()=>[m(d,{onClick:c[4]||=e=>o.value=!o.value},{default:e(()=>[...c[18]||=[t(`Open Dropdown`,-1)]]),_:1})]),default:e(()=>[a(`div`,{class:`dropdown-item`,onClick:c[5]||=e=>o.value=!1},`Action One`),a(`div`,{class:`dropdown-item`,onClick:c[6]||=e=>o.value=!1},`Action Two`),a(`div`,{class:`dropdown-item selected`,onClick:c[7]||=e=>o.value=!1},`Selected Item`),a(`div`,{class:`dropdown-item danger`,onClick:c[8]||=e=>o.value=!1},`Danger Item`)]),_:1},8,[`modelValue`])]),_:1}),m(G,{code:`<!-- Dropdown with icon button trigger -->
<v-dropdown v-model="open">
  <template #trigger>
    <v-icon-button>
      <i-material-symbols:more-vert />
    </v-icon-button>
  </template>
  <div class="dropdown-item">Edit</div>
  <div class="dropdown-item danger">Delete</div>
</v-dropdown>`},{default:e(()=>[m(g,{modelValue:l.value,"onUpdate:modelValue":c[12]||=e=>l.value=e},{trigger:e(()=>[m(b,null,{default:e(()=>[m(_)]),_:1})]),default:e(()=>[a(`div`,{class:`dropdown-item`,onClick:c[10]||=e=>l.value=!1},`Edit`),a(`div`,{class:`dropdown-item danger`,onClick:c[11]||=e=>l.value=!1},`Delete`)]),_:1},8,[`modelValue`])]),_:1})])}}}),le=[`tabindex`],ue={key:0,class:`v-input-chip__icon`},de={class:`v-input-chip__label`},fe=[`disabled`,`aria-label`],pe=_(i({__name:`VInputChip`,props:{label:{default:``},selected:{type:Boolean,default:!1},disabled:{type:Boolean,default:!1},removeOnly:{type:Boolean,default:!1},ariaLabelRemove:{default:``}},emits:[`remove`,`click`],setup(e,{emit:t}){let i=e,o=t;function c(){!i.disabled&&!i.removeOnly&&o(`click`)}function d(){i.disabled||o(`remove`)}function f(e){(e.key===`Enter`||e.key===` `)&&(e.preventDefault(),c())}return(t,i)=>(r(),s(`div`,{class:u([`v-input-chip`,{"v-input-chip--selected":e.selected,"v-input-chip--disabled":e.disabled,"v-input-chip--remove-only":e.removeOnly}]),tabindex:e.disabled?-1:0,onClick:c,onKeydown:f},[t.$slots.default?(r(),s(`span`,ue,[n(t.$slots,`default`,{},void 0,!0)])):h(``,!0),a(`span`,de,p(e.label),1),a(`button`,{class:`v-input-chip__remove`,type:`button`,disabled:e.disabled,"aria-label":e.ariaLabelRemove||`Remove`,onClick:l(d,[`stop`])},[...i[0]||=[a(`svg`,{viewBox:`0 0 24 24`,width:`18`,height:`18`},[a(`path`,{d:`M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z`})],-1)]],8,fe)],42,le))}}),[[`__scopeId`,`data-v-aa119b53`]]),me={class:`ux-section`},he={class:`top-app-bar`,style:{width:`100%`}},ge={class:`actions`},_e=i({__name:`UxDataDisplay`,setup(t){let n=c(`all`);return(t,i)=>{let o=F,c=P,l=pe,u=C,d=y,f=w,p=O;return r(),s(`section`,me,[i[6]||=a(`h2`,null,`Chips & Data Display`,-1),i[7]||=a(`h3`,null,`<v-filter-chip> + <v-chip-set>`,-1),m(G,{code:`<v-chip-set>
  <v-filter-chip label="All" :selected="filter === 'all'" @click="filter = 'all'" />
  <v-filter-chip label="Active" :selected="filter === 'active'" @click="filter = 'active'" />
</v-chip-set>`},{default:e(()=>[m(c,null,{default:e(()=>[m(o,{label:`All`,selected:n.value===`all`,onClick:i[0]||=e=>n.value=`all`},null,8,[`selected`]),m(o,{label:`Active`,selected:n.value===`active`,onClick:i[1]||=e=>n.value=`active`},null,8,[`selected`]),m(o,{label:`Archived`,selected:n.value===`archived`,onClick:i[2]||=e=>n.value=`archived`},null,8,[`selected`])]),_:1})]),_:1}),i[8]||=a(`h3`,null,`<v-input-chip>`,-1),m(G,{code:`<v-input-chip label="Tag" @remove="..." />`},{default:e(()=>[m(l,{label:`Photo`,onRemove:()=>{}}),m(l,{label:`Video`,selected:``,onRemove:()=>{}}),m(l,{label:`Disabled`,disabled:``})]),_:1}),i[9]||=a(`h3`,null,`CSS: .card / .card.outlined`,-1),m(G,{code:`<div class="card">Default card</div>
<div class="card outlined">Outlined card</div>
<div class="card selected">Selected card</div>`},{default:e(()=>[...i[3]||=[a(`div`,{class:`card`,style:{flex:`1`}},`Default card content`,-1),a(`div`,{class:`card outlined`,style:{flex:`1`}},`Outlined card content`,-1),a(`div`,{class:`card selected`,style:{flex:`1`}},`Selected card content`,-1)]]),_:1}),i[10]||=a(`h3`,null,`CSS: .selectable-card`,-1),m(G,{code:`<div class="selectable-card">Normal</div>
<div class="selectable-card selected">Selected</div>
<div class="selectable-card selecting">Selecting</div>`},{default:e(()=>[...i[4]||=[a(`div`,{class:`selectable-card`,style:{padding:`12px`,flex:`1`}},`Normal`,-1),a(`div`,{class:`selectable-card selected`,style:{padding:`12px`,flex:`1`}},`Selected`,-1),a(`div`,{class:`selectable-card selecting`,style:{padding:`12px`,flex:`1`}},`Selecting`,-1)]]),_:1}),i[11]||=a(`h3`,null,`CSS: .top-app-bar`,-1),m(G,{code:`<div class="top-app-bar">
  <v-icon-button>...</v-icon-button>
  <div class="title">Page Title</div>
  <div class="actions">...</div>
</div>`},{default:e(()=>[a(`div`,he,[m(d,null,{default:e(()=>[m(u)]),_:1}),i[5]||=a(`div`,{class:`title`},`Page Title`,-1),a(`div`,ge,[m(d,null,{default:e(()=>[m(f)]),_:1}),m(d,null,{default:e(()=>[m(p)]),_:1})])])]),_:1})])}}}),ve={class:`ux-section`},ye={style:{display:`flex`,"flex-direction":`column`,gap:`8px`,width:`100%`}},be={style:{display:`flex`,gap:`16px`,"align-items":`flex-start`}},xe={key:0,class:`card`,style:{padding:`8px`}},Se={key:0,class:`card`,style:{padding:`8px`,overflow:`hidden`}},Ce={key:0,class:`card`,style:{padding:`8px`,overflow:`hidden`}},we={class:`nav`,style:{width:`200px`}},Te={class:`active`},Ee={class:`icon`},De={class:`icon`},$={class:`icon`},Oe=i({__name:`UxCssUtilities`,setup(n){let i=c(!0),o=c(3);return(n,c)=>{let l=x,u=A,f=M,g=ee,_=N;return r(),s(`section`,ve,[c[12]||=a(`h2`,null,`CSS Utilities`,-1),c[13]||=a(`h3`,null,`.skeleton-text / .skeleton-image`,-1),m(G,{code:`<div class="skeleton-text" style="width: 60%" />
<div class="skeleton-text lg" style="width: 40%" />
<div class="skeleton-image lg" />
<div class="skeleton-checkbox" />`},{default:e(()=>[...c[1]||=[a(`div`,{style:{display:`flex`,gap:`12px`,"align-items":`center`,width:`100%`}},[a(`div`,{class:`skeleton-image lg`}),a(`div`,{style:{flex:`1`,display:`flex`,"flex-direction":`column`,gap:`8px`}},[a(`div`,{class:`skeleton-text`,style:{width:`70%`}}),a(`div`,{class:`skeleton-text lg`,style:{width:`45%`}})]),a(`div`,{class:`skeleton-checkbox`})],-1)]]),_:1}),c[14]||=a(`h3`,null,`.nowrap`,-1),m(G,{code:`<span class="nowrap">Text that won't wrap</span>`},{default:e(()=>[...c[2]||=[a(`span`,{class:`nowrap`},`This text will not wrap to a new line, no matter what.`,-1)]]),_:1}),c[15]||=a(`h3`,null,`Vue Transitions: fade / width / height`,-1),m(G,{code:`<transition name="fade">
  <div v-if="show">Fades in/out</div>
</transition>

<transition name="width">
  <div v-if="show">Width transition</div>
</transition>

<transition name="height">
  <div v-if="show">Height transition</div>
</transition>`},{default:e(()=>[a(`div`,ye,[m(l,{class:`btn-sm`,onClick:c[0]||=e=>i.value=!i.value},{default:e(()=>[t(` Toggle (`+p(i.value?`visible`:`hidden`)+`) `,1)]),_:1}),a(`div`,be,[a(`div`,null,[c[3]||=a(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`fade:`,-1),m(d,{name:`fade`},{default:e(()=>[i.value?(r(),s(`div`,xe,`Fade`)):h(``,!0)]),_:1})]),a(`div`,null,[c[4]||=a(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`width:`,-1),m(d,{name:`width`},{default:e(()=>[i.value?(r(),s(`div`,Se,`Width`)):h(``,!0)]),_:1})]),a(`div`,null,[c[5]||=a(`div`,{style:{"font-size":`0.75rem`,"margin-bottom":`4px`,color:`var(--md-sys-color-on-surface-variant)`}},`height:`,-1),m(d,{name:`height`},{default:e(()=>[i.value?(r(),s(`div`,Ce,`Height`)):h(``,!0)]),_:1})])])])]),_:1}),c[16]||=a(`h3`,null,`.surface-card`,-1),m(G,{code:`<div class="surface-card">Surface card with background</div>`},{default:e(()=>[...c[6]||=[a(`div`,{class:`surface-card`,style:{flex:`1`}},`Surface card content with default padding`,-1)]]),_:1}),c[17]||=a(`h3`,null,`.nav (sidebar navigation)`,-1),m(G,{code:`<ul class="nav">
  <li class="active">
    <div class="icon">📁</div>
    <div class="title">Files</div>
    <div class="count">12</div>
  </li>
</ul>`},{default:e(()=>[a(`ul`,we,[a(`li`,Te,[a(`div`,Ee,[m(u)]),c[7]||=a(`div`,{class:`title`},`Files`,-1),c[8]||=a(`div`,{class:`count`},`12`,-1)]),a(`li`,null,[a(`div`,De,[m(f)]),c[9]||=a(`div`,{class:`title`},`Images`,-1),c[10]||=a(`div`,{class:`count`},`48`,-1)]),a(`li`,null,[a(`div`,$,[m(g)]),c[11]||=a(`div`,{class:`title`},`Audio`,-1)])])]),_:1}),c[18]||=a(`h3`,null,`.pagination`,-1),m(G,{code:`<v-pagination :total="100" :limit="10" :page="3" :go="goToPage" />`},{default:e(()=>[m(_,{total:100,limit:10,page:o.value,go:e=>o.value=e},null,8,[`page`,`go`])]),_:1})])}}}),ke={class:`ux-md-editor`},Ae={class:`toolbar`},je={class:`editor-frame`},Me=_(i({__name:`UxMarkdownEditor`,setup(e){let n=c(`# 产品设计评审

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

![架构图](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='360'%20height='120'%3E%3Crect%20width='360'%20height='120'%20fill='%233f51b5'/%3E%3Ctext%20x='180'%20y='68'%20font-size='22'%20fill='white'%20text-anchor='middle'%20font-family='sans-serif'%3EPlainDesk%20Architecture%3C/text%3E%3C/svg%3E)

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
`),i=c(document.documentElement.classList.contains(`dark`));function o(){i.value=!i.value,document.documentElement.classList.toggle(`dark`,i.value),k.emit(`color_mode_changed`)}return(e,c)=>(r(),s(`section`,ke,[c[1]||=a(`h2`,null,`MarkdownEditor (Live Preview)`,-1),c[2]||=a(`p`,{class:`hint`},[t(` 光标所在元素显示原始 Markdown 标记，移开后原地渲染；空行输入 `),a(`code`,null,`/`),t(` 呼出块插入菜单。 `)],-1),a(`div`,Ae,[a(`button`,{type:`button`,onClick:o},p(i.value?`☀️ Light`:`🌙 Dark`),1)]),a(`div`,je,[m(R,{modelValue:n.value,"onUpdate:modelValue":c[0]||=e=>n.value=e,placeholder:`Write markdown... (input / for blocks)`},null,8,[`modelValue`])])]))}}),[[`__scopeId`,`data-v-0bbb41b8`]]),Ne={class:`ux-page`},Pe={class:`ux-main`},Fe=_(i({__name:`UxView`,setup(e){return(e,t)=>(r(),s(`div`,Ne,[t[0]||=a(`header`,{class:`ux-header`},[a(`h1`,null,`UI Component Library`),a(`p`,null,`Base components and reusable CSS patterns. All components are globally registered with V-prefix.`)],-1),a(`main`,Pe,[m(J),m(Q),m(ie),m(ce),m(_e),m(Oe),m(Me)])]))}}),[[`__scopeId`,`data-v-b578e6b5`]]);export{Fe as default};