import{_ as y}from"./FieldId-c6154079.js";import{_ as I}from"./Breadcrumb-1d05f19b.js";import{d as D,p as M,r as V,u as F,i as B,t as E,Z as l,bj as m,V as N,o as r,c as d,b as e,e as f,f as t,F as q,y as S,g,K as j,N as A,w as $,z as L,Y as P}from"./index-655a4b95.js";import{p as U}from"./parser-fdd85e1d.js";import{_ as z}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-869cd02d.js";import"./VModal.vuevuetypescriptsetuptruelang-27b25cef.js";const K={class:"page-container container"},Q={class:"main"},Y={class:"table"},Z=e("th",null,"ID",-1),H={class:"actions two"},J={class:"form-check"},O=["disabled","onChange","onUpdate:modelValue"],R={class:"actions two"},T=["onClick"],X=["onClick"],ie=D({__name:"WireGuardView",setup(x){const b=M(),u=V([]),{t:c}=F();function w(n){L(b,`/wireguard/${n}`)}B({handle:(n,s)=>{s?E(c(s),"error"):u.value=n.wireGuards.map(o=>({...o,...U(o.config)}))},document:l`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${m}
  `});const{mutate:v,loading:k}=N({document:l`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${m}
  `});function G(n){v({id:n.id,enable:n.isEnabled})}function C(n){var s;P(z,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:l`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const o=I,W=y;return r(),d("div",K,[e("div",Q,[f(o,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("table",Y,[e("thead",null,[e("tr",null,[Z,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",H,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(q,null,S(u.value,a=>{var p,_,h;return r(),d("tr",{key:a.id},[e("td",null,[f(W,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((p=a.wgInterface)==null?void 0:p.name),1),e("td",null,t((h=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:h.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${g(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",J,[j(e("input",{class:"form-check-input",disabled:g(k),onChange:i=>G(a),"onUpdate:modelValue":i=>a.isEnabled=i,type:"checkbox"},null,40,O),[[A,a.isEnabled]])])]),e("td",R,[e("a",{href:"#",class:"v-link",onClick:$(i=>w(a.id),["prevent"])},t(n.$t("edit")),9,T),e("a",{href:"#",class:"v-link",onClick:$(i=>C(a),["prevent"])},t(n.$t("delete")),9,X)])])}),128))])])])])}}});export{ie as default};
