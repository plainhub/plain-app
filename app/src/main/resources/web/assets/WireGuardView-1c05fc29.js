import{d as I,e as y,r as D,u as F,h as M,y as q,ac as o,bZ as g,i as B,c as l,a as e,q as $,t,F as E,K as N,o as r,k as f,m,D as S,a3 as V,ad as A,a6 as L}from"./index-2583f876.js";import{_ as P}from"./Breadcrumb-ef080890.js";import{p as j}from"./parser-fdd85e1d.js";const K={class:"page-container"},Q={class:"main"},Z={class:"table-responsive"},z={class:"table"},H=e("th",null,"ID",-1),J={class:"actions two"},O={class:"form-check"},R=["disabled","onChange","checked"],T={class:"actions two"},U=["onClick"],X=["onClick"],ae=I({__name:"WireGuardView",setup(Y){const b=y(),d=D([]),{t:c}=F();function v(n){S(b,`/wireguard/${n}`)}M({handle:(n,s)=>{s?q(c(s),"error"):d.value=n.wireGuards.map(i=>({...i,...j(i.config)}))},document:o`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${g}
  `});const{mutate:w,loading:k}=B({document:o`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${g}
  `});function G(n){w({id:n.id,enable:n.isEnabled})}function W(n){var s;V(A,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:o`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const i=P,C=L;return r(),l("div",K,[e("div",Q,[$(i,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("div",Z,[e("table",z,[e("thead",null,[e("tr",null,[H,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",J,t(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),l(E,null,N(d.value,a=>{var u,_,p;return r(),l("tr",{key:a.id},[e("td",null,[$(C,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((u=a.wgInterface)==null?void 0:u.name),1),e("td",null,t((p=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:p.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${f(c)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",O,[e("md-checkbox",{"touch-target":"wrapper",disabled:f(k),onChange:h=>G(a),checked:a.isEnabled},null,40,R)])]),e("td",T,[e("a",{href:"#",class:"v-link",onClick:m(h=>v(a.id),["prevent"])},t(n.$t("edit")),9,U),e("a",{href:"#",class:"v-link",onClick:m(h=>W(a),["prevent"])},t(n.$t("delete")),9,X)])])}),128))])])])])])}}});export{ae as default};
