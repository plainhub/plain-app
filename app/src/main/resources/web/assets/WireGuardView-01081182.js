import{d as I,e as y,r as D,u as F,h as M,x as B,ac as o,bZ as g,i as q,c as l,a as e,p as $,t,F as E,J as N,o as r,k as f,w as m,C as S,a3 as V,ad as A,a6 as L}from"./index-e900249a.js";import{_ as P}from"./Breadcrumb-d9676db0.js";import{p as j}from"./parser-fdd85e1d.js";const J={class:"page-container"},Q={class:"main"},Z={class:"table-responsive"},z={class:"table"},H=e("th",null,"ID",-1),K={class:"actions two"},O={class:"form-check"},R=["disabled","onChange","checked"],T={class:"actions two"},U=["onClick"],X=["onClick"],ae=I({__name:"WireGuardView",setup(Y){const b=y(),d=D([]),{t:c}=F();function w(n){S(b,`/wireguard/${n}`)}M({handle:(n,s)=>{s?B(c(s),"error"):d.value=n.wireGuards.map(i=>({...i,...j(i.config)}))},document:o`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${g}
  `});const{mutate:v,loading:k}=q({document:o`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${g}
  `});function G(n){v({id:n.id,enable:n.isEnabled})}function C(n){var s;V(A,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:o`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const i=P,W=L;return r(),l("div",J,[e("div",Q,[$(i,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("div",Z,[e("table",z,[e("thead",null,[e("tr",null,[H,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",K,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),l(E,null,N(d.value,a=>{var u,_,p;return r(),l("tr",{key:a.id},[e("td",null,[$(W,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((u=a.wgInterface)==null?void 0:u.name),1),e("td",null,t((p=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:p.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${f(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",O,[e("md-checkbox",{"touch-target":"wrapper",disabled:f(k),onChange:h=>G(a),checked:a.isEnabled},null,40,R)])]),e("td",T,[e("a",{href:"#",class:"v-link",onClick:m(h=>w(a.id),["prevent"])},t(n.$t("edit")),9,U),e("a",{href:"#",class:"v-link",onClick:m(h=>C(a),["prevent"])},t(n.$t("delete")),9,X)])])}),128))])])])])])}}});export{ae as default};
