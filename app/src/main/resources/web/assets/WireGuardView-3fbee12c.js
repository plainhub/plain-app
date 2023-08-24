import{_ as I}from"./FieldId-e37ba71f.js";import{_ as y}from"./Breadcrumb-731a6814.js";import{d as M,p as D,r as F,u as V,i as B,t as E,a1 as l,bp as m,Y as q,o as r,c as d,b as e,e as f,f as t,F as A,z as N,g,M as P,P as S,w as $,A as L,a0 as U}from"./index-66bea2e9.js";import{p as j}from"./parser-fdd85e1d.js";import{_ as z}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-f7245844.js";import"./VModal.vuevuetypescriptsetuptruelang-091ab08d.js";const Q={class:"page-container container"},Y={class:"main"},H={class:"table"},J=e("th",null,"ID",-1),K={class:"actions two"},O={class:"form-check"},R=["disabled","onChange","onUpdate:modelValue"],T={class:"actions two"},X=["onClick"],Z=["onClick"],ie=M({__name:"WireGuardView",setup(x){const b=D(),u=F([]),{t:c}=V();function w(n){L(b,`/wireguard/${n}`)}B({handle:(n,s)=>{s?E(c(s),"error"):u.value=n.wireGuards.map(o=>({...o,...j(o.config)}))},document:l`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${m}
  `});const{mutate:v,loading:k}=q({document:l`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${m}
  `});function G(n){v({id:n.id,enable:n.isEnabled})}function C(n){var s;U(z,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:l`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const o=y,W=I;return r(),d("div",Q,[e("div",Y,[f(o,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("table",H,[e("thead",null,[e("tr",null,[J,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",K,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(A,null,N(u.value,a=>{var p,_,h;return r(),d("tr",{key:a.id},[e("td",null,[f(W,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((p=a.wgInterface)==null?void 0:p.name),1),e("td",null,t((h=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:h.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${g(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",O,[P(e("input",{class:"form-check-input",disabled:g(k),onChange:i=>G(a),"onUpdate:modelValue":i=>a.isEnabled=i,type:"checkbox"},null,40,R),[[S,a.isEnabled]])])]),e("td",T,[e("a",{href:"#",class:"v-link",onClick:$(i=>w(a.id),["prevent"])},t(n.$t("edit")),9,X),e("a",{href:"#",class:"v-link",onClick:$(i=>C(a),["prevent"])},t(n.$t("delete")),9,Z)])])}),128))])])])])}}});export{ie as default};
