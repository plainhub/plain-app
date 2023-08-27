import{_ as I}from"./FieldId-4280c3b8.js";import{_ as y}from"./Breadcrumb-99ff7c19.js";import{d as D,q as M,r as B,u as F,i as V,t as q,a2 as l,bs as m,Z as E,o as r,c as d,e,f,g as t,F as N,A,j as g,N as S,Q as j,w as $,B as L,a1 as P}from"./index-cf22a9d8.js";import{p as Q}from"./parser-fdd85e1d.js";import{_ as U}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-1943d7a1.js";import"./VModal.vuevuetypescriptsetuptruelang-83ac7574.js";const Z={class:"page-container container"},z={class:"main"},H={class:"table"},J=e("th",null,"ID",-1),K={class:"actions two"},O={class:"form-check"},R=["disabled","onChange","onUpdate:modelValue"],T={class:"actions two"},X=["onClick"],Y=["onClick"],ie=D({__name:"WireGuardView",setup(x){const b=M(),u=B([]),{t:c}=F();function w(n){L(b,`/wireguard/${n}`)}V({handle:(n,s)=>{s?q(c(s),"error"):u.value=n.wireGuards.map(o=>({...o,...Q(o.config)}))},document:l`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${m}
  `});const{mutate:v,loading:k}=E({document:l`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${m}
  `});function G(n){v({id:n.id,enable:n.isEnabled})}function C(n){var s;P(U,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:l`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const o=y,W=I;return r(),d("div",Z,[e("div",z,[f(o,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("table",H,[e("thead",null,[e("tr",null,[J,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",K,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(N,null,A(u.value,a=>{var p,_,h;return r(),d("tr",{key:a.id},[e("td",null,[f(W,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((p=a.wgInterface)==null?void 0:p.name),1),e("td",null,t((h=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:h.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${g(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",O,[S(e("input",{class:"form-check-input",disabled:g(k),onChange:i=>G(a),"onUpdate:modelValue":i=>a.isEnabled=i,type:"checkbox"},null,40,R),[[j,a.isEnabled]])])]),e("td",T,[e("a",{href:"#",class:"v-link",onClick:$(i=>w(a.id),["prevent"])},t(n.$t("edit")),9,X),e("a",{href:"#",class:"v-link",onClick:$(i=>C(a),["prevent"])},t(n.$t("delete")),9,Y)])])}),128))])])])])}}});export{ie as default};
