import{_ as I}from"./FieldId-da304ce7.js";import{_ as y}from"./Breadcrumb-23f5606f.js";import{d as M,n as D,r as F,u as V,i as B,t as E,Y as l,b9 as m,U as q,o as r,c as d,a as e,b as f,e as t,F as N,v as S,j as g,J as U,M as j,w as $,x as A,X as L}from"./index-89decb33.js";import{p as P}from"./parser-fdd85e1d.js";import{_ as J}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-5ca1a67e.js";import"./VModal.vuevuetypescriptsetuptruelang-6d140002.js";const Q={class:"page-container container"},X={class:"main"},Y={class:"table"},z=e("th",null,"ID",-1),H={class:"actions two"},K={class:"form-check"},O=["disabled","onChange","onUpdate:modelValue"],R={class:"actions two"},T=["onClick"],Z=["onClick"],ie=M({__name:"WireGuardView",setup(x){const b=D(),u=F([]),{t:c}=V();function v(n){A(b,`/wireguard/${n}`)}B({handle:(n,s)=>{s?E(c(s),"error"):u.value=n.wireGuards.map(o=>({...o,...P(o.config)}))},document:l`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${m}
  `});const{mutate:w,loading:k}=q({document:l`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${m}
  `});function G(n){w({id:n.id,enable:n.isEnabled})}function C(n){var s;L(J,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:l`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const o=y,W=I;return r(),d("div",Q,[e("div",X,[f(o,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("table",Y,[e("thead",null,[e("tr",null,[z,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",H,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(N,null,S(u.value,a=>{var p,_,h;return r(),d("tr",{key:a.id},[e("td",null,[f(W,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((p=a.wgInterface)==null?void 0:p.name),1),e("td",null,t((h=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:h.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${g(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",K,[U(e("input",{class:"form-check-input",disabled:g(k),onChange:i=>G(a),"onUpdate:modelValue":i=>a.isEnabled=i,type:"checkbox"},null,40,O),[[j,a.isEnabled]])])]),e("td",R,[e("a",{href:"#",class:"v-link",onClick:$(i=>v(a.id),["prevent"])},t(n.$t("edit")),9,T),e("a",{href:"#",class:"v-link",onClick:$(i=>C(a),["prevent"])},t(n.$t("delete")),9,Z)])])}),128))])])])])}}});export{ie as default};
